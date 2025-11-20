/*
 * Copyright 2023 HM Revenue & Customs
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
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, SheetErrors}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import services.ProcessCsvService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.validator.{ProcessODSService, StaxProcessor}
import utils.ERSUtil

import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
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

  private[controllers] def readFileOds(downloadUrl: String)(implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent]): Either[Result, StaxProcessor] = {
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
      val processor = new StaxProcessor(contentInputStream)
      Right(processor)
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
                       (implicit request: RequestWithOptionalEmpRefAndPAYE[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {

    clearErrorCache().flatMap { clearedSuccessfully =>
      if (clearedSuccessfully) {
        //These .get's are safe because the UploadedSuccessfully model is already validated as existing in the UpscanController
        sessionCacheService.fetch[UploadedSuccessfully](ersUtil.CALLBACK_DATA_KEY).flatMap { file =>
          readFileOds(file.get.downloadUrl).fold(
            err => {
              logger.error(s"[UploadController][showuploadODSFile] failed in readFileOds for scheme : $scheme")
              Future.successful(err)
            },
            (processor: StaxProcessor) => {
              val isFileValid: Boolean = processODSService.performODSUpload(file.get.name, processor)(scheme, messages)
              if (isFileValid) {
                Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
              }
              else {
                Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(false)))
              }
              // TODO: Come back to...
//              result.flatMap[Result] {
//                case Success(true) => Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
//                case Success(false) => Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(false)))
//                case Failure(t) => handleException(t)
//              }
            }
          )
        }
      } else {
        logger.error(s"[UploadController][showuploadODSFile] failed for clearErrorCache scheme : $scheme")
        Future.successful(getGlobalErrorPage(request, messages))
      }
    }
  }

  def handleException(t: Throwable)(implicit request: Request[AnyContent]): Future[Result] = {
    t match {
      case e: ERSFileProcessingException =>
        for {
          _ <- sessionCacheService.cache[String](ersUtil.FORMAT_ERROR_CACHE, e.message)
          _ <- sessionCacheService.cache[Seq[String]](ersUtil.FORMAT_ERROR_CACHE_PARAMS, e.optionalParams)
          _ <- sessionCacheService.cache[Boolean](ersUtil.FORMAT_ERROR_EXTENDED_CACHE, e.needsExtendedInstructions)
        } yield {
          Redirect(routes.CheckingServiceController.formatErrorsPage())
        }
      case upstreamError: UpstreamErrorResponse =>
        logger.error(
          s"[UploadController][handleException] " +
            s"Encountered an upstream error response when processing a CSV file: " +
            s"status ${upstreamError.statusCode} with message ${upstreamError.getMessage()}")
        Future(getGlobalErrorPage)
      case notERSProcessingException =>
        logger.error(s"[UploadController][handleException] " +
          s"Encountered unexpected exception: ${notERSProcessingException.getClass}. Redirecting to global error page.")
        Future(getGlobalErrorPage)
      case e: javax.xml.stream.XMLStreamException =>
        logger.error(s"[UploadController][handleException] " +
          s"Encountered unexpected exception: ${e.getClass}. Redirecting to global error page.")
        Future(getGlobalErrorPage)
    }
  }
}
