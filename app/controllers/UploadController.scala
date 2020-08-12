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

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL
import java.util.zip.ZipInputStream

import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, SheetErrors}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{CsvFileProcessor, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CacheUtil

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object UploadController extends UploadController {
	override val processODSService: ProcessODSService = ProcessODSService
	override val cacheUtil: CacheUtil = CacheUtil
	override val csvFileProcessor: CsvFileProcessor = CsvFileProcessor
	override val authAction: AuthAction = AuthAction
}

trait UploadController extends ERSCheckingBaseController {

	val processODSService: ProcessODSService
	val cacheUtil: CacheUtil
	val csvFileProcessor: CsvFileProcessor
	val authAction: AuthAction

	def downloadAsInputStream(downloadUrl: String): InputStream = new URL(downloadUrl).openStream()

	def clearCache()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Boolean] = {
		//remove function doesn't work, the cache needs to be overwritten with 'blank' data
		(for {
			_  	<-	cacheUtil.cache[Long](CacheUtil.SCHEME_ERROR_COUNT_CACHE, 0L)
			_	 	<-	cacheUtil.cache[ListBuffer[SheetErrors]](CacheUtil.ERROR_LIST_CACHE, new ListBuffer[SheetErrors]())
		} yield {
			Logger.debug(s"[UploadController][clearCache] Successfully cleared cache")
			true
		}) recoverWith {
			case e: Exception =>
				Logger.error(s"[UploadController][clearCache] Failed to clear cache of errors and error count", e)
				Future.successful(false)
		}
	}

	private[controllers] def readFileCsv(downloadUrl: String)(implicit ec: ExecutionContext): Future[Iterator[String]] = {
		try {
			val reader = new BufferedReader(new InputStreamReader(downloadAsInputStream(downloadUrl)))
			Future(reader.lines().iterator().asScala)
		} catch {
			case _: Throwable => throw ERSFileProcessingException("Failed to stream the data from file", "Exception bulk entity streaming")
		}
	}

	private[controllers] def readFileOds(downloadUrl: String): StaxProcessor = {
		val stream: InputStream = downloadAsInputStream(downloadUrl)
		val targetFileName = "content.xml"
		val zipInputStream: ZipInputStream = new ZipInputStream(stream)

		@scala.annotation.tailrec
    def findFileInZip(stream: ZipInputStream): InputStream = {
			Option(stream.getNextEntry) match {
				case Some(entry) if entry.getName == targetFileName =>
					stream
				case Some(_) =>
					findFileInZip(stream)
				case None =>
					throw ERSFileProcessingException(
						"Failed to stream the data from file",
						"Exception bulk entity streaming"
					)
			}
		}
		val contentInputStream = findFileInZip(zipInputStream)
		new StaxProcessor(contentInputStream)
	}

	def uploadCSVFile(scheme: String): Action[AnyContent] = authAction.async {
		implicit request =>
			showuploadCSVFile(scheme)
	}

	def showuploadCSVFile(scheme: String)(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {
		clearCache() map {
			case false => getGlobalErrorPage(request,messages)
		}

		cacheUtil.shortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](cacheUtil.getCacheId, "callback_data_key_csv") flatMap { callback =>
			val processFiles = callback.get.files map { file =>
				val successUpload = file.uploadStatus.asInstanceOf[UploadedSuccessfully]
				readFileCsv(successUpload.downloadUrl) flatMap { iterator =>
					csvFileProcessor.processCsvUpload(iterator, successUpload.name, scheme, file)(request, hc, messages) flatMap {
						case Success(true)	  =>
							Future.successful(true)
						case Success(false) 	=>
							Future.successful(false)
							case Failure(e) =>
								Future.failed(e)
					}
				}
			}

			Future.sequence(processFiles).map { list =>
				if (list.forall(identity)) {
					Redirect(routes.CheckingServiceController.checkingSuccessPage())
				} else {
					Redirect(routes.HtmlReportController.htmlErrorReportPage(true))
				}
			} recoverWith {
				case t: Throwable =>
					handleException(t)
			}
		}
	}



	def uploadODSFile(scheme: String): Action[AnyContent] = authAction.async {
		implicit request =>
			showuploadODSFile(scheme)
	}



	def showuploadODSFile(scheme: String)
											 (implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {
		clearCache() map {
			case false => getGlobalErrorPage(request, messages)
		}

		//These .get's are safe because the UploadedSuccessfully model is already validated as existing in the UpscanController
		cacheUtil.shortLivedCache.fetchAndGetEntry[UploadedSuccessfully](cacheUtil.getCacheId, "callback_data_key") flatMap { file =>
			val result = processODSService.performODSUpload(file.get.name, readFileOds(file.get.downloadUrl))(request, scheme, hc, messages)
			result.flatMap[Result] {
				case Success(true) => Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
				case Success(false) => Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(false)))
				case Failure(t) => handleException(t)
			}
		}
	}

	def handleException(t: Throwable)(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {
		t match {
			case e: ERSFileProcessingException =>
				for {
					_ <- cacheUtil.cache[String](CacheUtil.FORMAT_ERROR_CACHE, e.message)
					_ <- cacheUtil.cache[Seq[String]](CacheUtil.FORMAT_ERROR_CACHE_PARAMS, e.optionalParams)
					_ <- cacheUtil.cache[Boolean](CacheUtil.FORMAT_ERROR_EXTENDED_CACHE, e.needsExtendedInstructions)
				} yield {
					Redirect(routes.CheckingServiceController.formatErrorsPage())
				}
			case _ => throw t
		}
	}

	def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
		Ok(views.html.global_error(
			messages("ers.global_errors.title"),
			messages("ers.global_errors.heading"),
			messages("ers.global_errors.message"))(request, messages))
	}

}
