/*
 * Copyright 2026 HM Revenue & Customs
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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl._
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.stream.scaladsl.Source
import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalEmpRefAndPAYE}
import models.ERSFileProcessingException
import models.SheetErrors.format
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallbackList}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import services.{ProcessCsvService, ProcessODSService}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.validator.{DataContainsAmpersandException, IncorrectHeaderException, IncorrectSchemeException, IncorrectSheetNameException, NoDataException, ValidatorException}

import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.validator.models.ods.SheetErrors
import utils.ContentUtil.withArticle
import utils.ERSUtil
import utils.UploadedFileUtil.checkODSFileType

import scala.concurrent.impl.Promise
import scala.util.{Failure, Success}

@Singleton
class UploadController @Inject()(authAction: AuthAction,
                                 processODSService: ProcessODSService,
                                 processCsvService: ProcessCsvService,
                                 sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error
                                )(implicit executionContext: ExecutionContext,
                                  actorSystem: ActorSystem, val ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with ErsBaseController with Logging {

  def downloadAsInputStream(downloadUrl: String): InputStream = new URL(downloadUrl).openStream()

  def clearErrorCache()(implicit request: Request[_]): Future[Boolean] = {
    //remove function doesn't work, the cache needs to be overwritten with 'blank' data
    (for {
      _ <- sessionCacheService.cache[Long](ersUtil.SCHEME_ERROR_COUNT_CACHE, 0L)
      _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](ersUtil.ERROR_LIST_CACHE, new ListBuffer[SheetErrors]())
    } yield {
      logger.debug(s"[UploadController][clearCache] Successfully cleared cache")
      true
    }) recoverWith {
      case e: Exception =>
        logger.error(s"[UploadController][clearCache] Failed to clear cache of errors and error count", e)
        Future.successful(false)
    }
  }

  private[controllers] def readFileCsv(downloadUrl: String): Source[HttpResponse, _] = {
    Source
      .single(HttpRequest(uri = downloadUrl))
      .mapAsync(parallelism = 1)(makeRequest)
  }

  private[controllers] def makeRequest(request: HttpRequest): Future[HttpResponse] = Http()(actorSystem).singleRequest(request)

  private[controllers] def readFileOds(downloadUrl: String)(implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent]): Either[Result, InputStream] = {
    try {
      val stream: InputStream = downloadAsInputStream(downloadUrl)
      val targetFileName = "content.xml"
      val zipInputStream = new ZipInputStream(stream)
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
      val contentInputStream: InputStream = findFileInZip(zipInputStream)
      Right(contentInputStream)
    } catch {
      case e: ERSFileProcessingException =>
        logger.error(s"[UploadController][readFileOds] Error processing ODS file: ${e.getMessage}", e)
        Left(getGlobalErrorPage)

      case e: Exception =>
        logger.error(s"[UploadController][readFileOds] Unexpected error while reading ODS file: ${e.getMessage}", e)
        Left(getGlobalErrorPage)
    }
  }

  def uploadCSVFile(scheme: String): Action[AnyContent] = authAction.async {
    implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent] =>
      clearErrorCache() flatMap {
        case false => Future(getGlobalErrorPage)
        case _ =>
          sessionCacheService.fetch[UpscanCsvFilesCallbackList](ersUtil.CALLBACK_DATA_KEY_CSV) flatMap { callback =>
            val validationResults = processCsvService.processFiles(callback, scheme, readFileCsv)
            finaliseRequestAndRedirect(validationResults)
          }
      }
  }

  def finaliseRequestAndRedirect(validationResults: List[Future[Either[Throwable, Boolean]]])(
    implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent]): Future[Result] =
    Future.sequence(validationResults).flatMap {
      case noFailures if noFailures.forall(_.contains(true)) =>
        Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
      case failures  =>
        failures.find(_.isLeft) match {
          case Some(Left(exception)) => handleException(exception)
          case _ =>
            Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(true)))
        }

    }

  def uploadODSFile(scheme: String): Action[AnyContent] = authAction.async {
    implicit request =>
      showuploadODSFile(scheme)
  }

  def showuploadODSFile(scheme: String)
                       (implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent], messages: Messages): Future[Result] = {

    clearErrorCache().flatMap { clearedSuccessfully =>
      if (clearedSuccessfully) {
        //These .get's are safe because the UploadedSuccessfully model is already validated as existing in the UpscanController
        sessionCacheService.fetch[UploadedSuccessfully](ersUtil.CALLBACK_DATA_KEY).flatMap { file =>
          val fileName: String = file.get.name
          if (checkODSFileType(fileName)) {
            readFileOds(file.get.downloadUrl).fold(
              err => {
                logger.error(s"[UploadController][showuploadODSFile] failed in readFileOds for scheme : $scheme")
                Future.successful(err)
              },
              processor => {
                val fileName: String = file.get.name
                  processODSService
                    .performODSUpload(appConfig.errorCount, fileName, processor, scheme)(request)
                    .flatMap {
                      case Right(fileIsValid: Boolean) =>
                        if (fileIsValid) {
                          Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
                        } else {
                          Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(false)))
                        }
                      case Left(e: Exception) => handleException(e)
                    }
                }
            )
          } else {
            val exception: ERSFileProcessingException = ERSFileProcessingException(
              messages("ers_check_file.file_type_error", fileName),
              messages("ers_check_file.file_type_error", fileName)
            )
            logger.error(s"[showuploadODSFile]] Unable to save File Name. Error: ${exception.getMessage}")
            handleException(exception)
          }
        }
      } else {
        logger.error(s"[UploadController][showuploadODSFile] failed for clearErrorCache scheme : $scheme")
        Future.successful(getGlobalErrorPage(request, messages))
      }
    }
  }

  def updateCacheThenRedirectToFormatErrorsPage(message: String, params: Seq[String], needsExtendedInstructions: Boolean)(implicit request: Request[AnyContent]): Future[Result] = {
    for {
      _ <- sessionCacheService.cache[String](ersUtil.FORMAT_ERROR_CACHE, message)
      _ <- sessionCacheService.cache[Seq[String]](ersUtil.FORMAT_ERROR_CACHE_PARAMS, params)
      _ <- sessionCacheService.cache[Boolean](ersUtil.FORMAT_ERROR_EXTENDED_CACHE, needsExtendedInstructions)
    } yield {
      Redirect(routes.CheckingServiceController.formatErrorsPage())
    }
  }


  def handleException(t: Throwable)(implicit request: Request[AnyContent]): Future[Result] = {
    t match {
      case e: ERSFileProcessingException =>
        updateCacheThenRedirectToFormatErrorsPage(e.message, e.optionalParams, e.needsExtendedInstructions)
      case upstreamError: UpstreamErrorResponse =>
        logger.error(
          s"[UploadController][handleException] " +
            s"Encountered an upstream error response when processing a CSV file: " +
            s"status ${upstreamError.statusCode} with message ${upstreamError.getMessage()}")
        Future(getGlobalErrorPage)
      case e: javax.xml.stream.XMLStreamException =>
        logger.error(s"[UploadController][handleException] " +
          s"Encountered unexpected exception: ${e.getClass}. Redirecting to global error page.")
        Future(getGlobalErrorPage)
      case e: IncorrectSchemeException =>
        val selectedSchemeTypeWithArticle: String = withArticle(e.selectedSchemeType.toUpperCase())
        val uploadedFileSchemeTypeWithArticle: String = withArticle(e.uploadedFileSchemeType.toUpperCase())
        val message = Messages("ers.exceptions.dataParser.incorrectSchemeType", selectedSchemeTypeWithArticle, uploadedFileSchemeTypeWithArticle, e.fileName)
        val optionalParams = Seq(selectedSchemeTypeWithArticle, uploadedFileSchemeTypeWithArticle, e.fileName)
        updateCacheThenRedirectToFormatErrorsPage(message, optionalParams, needsExtendedInstructions = false)
      case _: NoDataException =>
        updateCacheThenRedirectToFormatErrorsPage(
          Messages("ers.exceptions.dataParser.noData"),
          Seq.empty[String],
          needsExtendedInstructions = true
        )
      case _: DataContainsAmpersandException =>
        updateCacheThenRedirectToFormatErrorsPage(
          Messages("ers.exceptions.dataParser.ampersand"),
          Seq.empty[String],
          needsExtendedInstructions = true
        )
      case e: IncorrectHeaderException =>
        val message = Messages("ers.exceptions.dataParser.incorrectHeader", e.sheetName, e.fileName)
        val optionalParams = Seq(e.sheetName, e.fileName)
        updateCacheThenRedirectToFormatErrorsPage(message, optionalParams, needsExtendedInstructions = true)
      case e: IncorrectSheetNameException =>
        val message = Messages("ers.exceptions.dataParser.incorrectSheetName", e.sheetName, e.schemeName)
        val optionalParams = Seq(e.sheetName, e.schemeName)
        updateCacheThenRedirectToFormatErrorsPage(message, optionalParams, needsExtendedInstructions = true)
      case e: ValidatorException =>
        logger.error(s"[UploadController][handleException] " +
          s"Encountered unexpected exception: ${e.getClass}. Redirecting to global error page.")
        Future(getGlobalErrorPage)
      case notERSProcessingException: Throwable => // Catch all case
        logger.error(s"[UploadController][handleException] " +
          s"Encountered unexpected exception: ${notERSProcessingException.getClass}. Redirecting to global error page.")
        Future(getGlobalErrorPage)
    }
  }
}
