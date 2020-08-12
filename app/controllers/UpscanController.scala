/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.ApplicationConfig
import controllers.auth.AuthAction
import models.upscan._
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import services.{CsvFileProcessor, ProcessODSService, SessionService}
import utils.{CacheUtil, Retryable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UpscanController extends ERSCheckingBaseController with Retryable with LegacyI18nSupport {

	val authAction: AuthAction
	val sessionService: SessionService
	val processODSService: ProcessODSService
	val processCSVService: CsvFileProcessor

	def failure(): Action[AnyContent] = authAction.async { implicit request =>
		Logger.error(s"[UpscanController][failure] Failed to upload file to Upscan")
		Future.successful(getGlobalErrorPage)
	}

	def successCSV(uploadId: UploadId, scheme: String): Action[AnyContent] = authAction.async { implicit request =>
		Logger.debug(s"[UpscanController][successCSV] Upload form submitted for ID: $uploadId")
		val sessionId = hc.sessionId.get.value
		val upscanCsvFilesList = for {
			csvFileList   	<- sessionService.cacheUtil.fetch[UpscanCsvFilesList](CacheUtil.CSV_FILES_UPLOAD, sessionId)
			updatedCacheFileList = {
				Logger.info(s"[UpscanController][successCSV] Updating uploadId: ${uploadId.value} to InProgress")
				csvFileList.updateToInProgress(uploadId)
			}
			_ 							<- sessionService.cacheUtil.cache(CacheUtil.CSV_FILES_UPLOAD, updatedCacheFileList, sessionId)
		} yield updatedCacheFileList

		upscanCsvFilesList flatMap  { fileList =>
			if(fileList.noOfFilesToUpload == fileList.noOfUploads) {
				sessionService.getCallbackRecordCsv(sessionId).withRetry(ApplicationConfig.allCsvFilesCacheRetryAmount){ list =>
					Logger.debug(s"[UpscanController][successCSV] Comparing cached files [${list.files.size}] to numberOfFileToUpload[${fileList.noOfFilesToUpload}]")
					list.files.size == fileList.noOfFilesToUpload
				} map { callbackData =>
					if(callbackData.areAllFilesComplete() && callbackData.areAllFilesSuccessful()) {
						Redirect(routes.UploadController.uploadCSVFile(scheme))
					} else {
						Logger.error(s"[UpscanController][successCSV] Not all files are completed uploading - (${callbackData.areAllFilesComplete()}) " +
							s"or  had a successful response - (${callbackData.areAllFilesSuccessful()})")
						getGlobalErrorPage
					}
				} recover {
					case e: Exception =>
						Logger.error(s"success: failed to fetch callback data with exception ${e.getMessage}," +
							s"timestamp: ${System.currentTimeMillis()}.", e)
						getGlobalErrorPage
				}
			} else {
				Future.successful(Redirect(routes.CheckingServiceController.checkCSVFilePage()))
			}
		}

	}


	def successODS(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
		val futureCallbackData: Future[Option[UploadStatus]] = sessionService.getCallbackRecord.withRetry(ApplicationConfig.odsSuccessRetryAmount) {
			_.fold(true) {
					case _: UploadedSuccessfully | Failed => true
					case _ => false
				}
		}

		futureCallbackData flatMap {
			case Some(_: UploadedSuccessfully) =>
				Future.successful(Redirect(routes.UploadController.uploadODSFile(scheme)))
			case Some(Failed) =>
				Logger.warn("[UpscanController][successODS] Upload status is failed")
				Future.successful(getGlobalErrorPage)
			case None =>
				Logger.error(s"[UpscanController][successODS] Failed to verify upload. No data found in cache")
				Future.successful(getGlobalErrorPage)
		} recover {
			case e: LoopException[Option[UploadStatus]] =>
				Logger.error(s"[UpscanController][successODS] Failed to verify upload. Upload status: ${e.finalFutureData.flatten}", e)
				getGlobalErrorPage
			case e: Exception =>
				Logger.error(s"[UpscanController][successODS] Failed to save ods file with exception ${e.getMessage}.", e)
				getGlobalErrorPage
		}
	}

	def getGlobalErrorPage()(implicit request: Request[_]): Result = {
		Ok(views.html.global_error(
			Messages("ers.global_errors.title"),
			Messages("ers.global_errors.heading"),
			Messages("ers.global_errors.message")
		))
	}
}


object UpscanController extends UpscanController {
	override val authAction: AuthAction = AuthAction
	override val sessionService: SessionService = SessionService
	override val processODSService: ProcessODSService = ProcessODSService
	override val processCSVService: CsvFileProcessor = CsvFileProcessor
}

