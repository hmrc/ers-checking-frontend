/*
 * Copyright 2021 HM Revenue & Customs
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
import javax.inject.Inject
import models.upscan._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{ERSUtil, Retryable}
import scala.concurrent.{ExecutionContext, Future}

@Inject
class UpscanController @Inject()(authAction: AuthAction,
                                 sessionService: SessionService,
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error
                                )(implicit executionContext: ExecutionContext, ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with Retryable with I18nSupport with BaseController {

  def failure(): Action[AnyContent] = authAction.async { implicit request =>
    Logger.error("[UpscanController][failure] Failed to upload file to Upscan")
    Future.successful(getGlobalErrorPage)
  }


  def fetchCsvCallbackList(list: UpscanCsvFilesList, sessionId: String)
                          (implicit hc: HeaderCarrier, request: Request[_]): Future[Seq[UpscanCsvFilesCallback]] = {
    Future.sequence {
      list.ids map { head =>
        ersUtil.fetch[UpscanIds](head.uploadId.value, sessionId) map { upscanId =>
          UpscanCsvFilesCallback(upscanId.uploadId, upscanId.uploadStatus)
        }
      }
    }
}

  def successCSV(uploadId: UploadId, scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    Logger.info(s"[UpscanController][successCSV] Upload form submitted for ID: $uploadId")
    val sessionId = hc.sessionId.get.value

    val upscanCsvFilesList = for {
      csvFileList   <- ersUtil.fetch[UpscanCsvFilesList](ersUtil.CSV_FILES_UPLOAD, sessionId)
      updatedCacheFileList = {
        Logger.info(s"[UpscanController][successCSV] Updating uploadId: ${uploadId.value} to InProgress")
        csvFileList.updateToInProgress(uploadId)
      }
      _ <- ersUtil.cache(ersUtil.CSV_FILES_UPLOAD, updatedCacheFileList, sessionId)
    } yield updatedCacheFileList

    upscanCsvFilesList flatMap { fileList =>
      if(fileList.noOfFilesToUpload == fileList.noOfUploads) {
        fetchCsvCallbackList(fileList, sessionId).withRetry(appConfig.allCsvFilesCacheRetryAmount){ list =>
          Logger.debug(s"[UpscanController][successCSV] Fetched Callback list - $list")
          Logger.info(s"[UpscanController][successCSV] Comparing cached files [${list.size}] to numberOfFileToUpload[${fileList.noOfFilesToUpload}]")
          Logger.info(s"[UpscanController][successCSV] Checking if all files have completed upload - [${list.forall(_.isComplete)}]")


          (list.size == fileList.noOfFilesToUpload) && list.forall(_.isComplete)
        } map { files =>
          val callbackData = UpscanCsvFilesCallbackList(files.toList.reverse)
          sessionService.createCallbackRecordCSV(callbackData, sessionId)
          if(callbackData.areAllFilesSuccessful()) {
            Redirect(routes.UploadController.uploadCSVFile(scheme))
          } else {
            Logger.error(s"[UpscanController][successCSV] Not all files are completed uploading - (${callbackData.areAllFilesComplete()}) " +
              s"or  had a successful response - (${callbackData.areAllFilesSuccessful()})")
            getGlobalErrorPage
          }
        } recover {
          case e: Exception =>
            Logger.error(s"[UpscanController][successCSV] failed to fetch callback data with exception ${e.getMessage}," +
              s"timestamp: ${java.time.LocalTime.now()}.", e)
            getGlobalErrorPage
        }
      } else {
        Future.successful(Redirect(routes.CheckingServiceController.checkCSVFilePage()))
      }
    }

  }


  def successODS(scheme: String): Action[AnyContent] = authAction.async { implicit request =>
    val futureCallbackData: Future[Option[UploadStatus]] = sessionService.getCallbackRecord.withRetry(appConfig.odsSuccessRetryAmount) {
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
}
