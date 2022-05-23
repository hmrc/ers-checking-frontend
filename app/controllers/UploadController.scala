/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Source
import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, SheetErrors}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{ProcessCsvService, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
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
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error
                                )(implicit executionContext: ExecutionContext,
                                  actorSystem: ActorSystem, val ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with BaseController with Logging {

  def downloadAsInputStream(downloadUrl: String): InputStream = new URL(downloadUrl).openStream()

  def clearErrorCache()(implicit hc: HeaderCarrier): Future[Boolean] = {
    //remove function doesn't work, the cache needs to be overwritten with 'blank' data
    (for {
      _ <- ersUtil.cache[Long](ersUtil.SCHEME_ERROR_COUNT_CACHE, 0L)
      _ <- ersUtil.cache[ListBuffer[SheetErrors]](ersUtil.ERROR_LIST_CACHE, new ListBuffer[SheetErrors]())
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

    val contentInputStream: InputStream = findFileInZip(zipInputStream)
    new StaxProcessor(contentInputStream)
  }

  def uploadCSVFile(scheme: String): Action[AnyContent] = authAction.async {
    implicit request =>
      clearErrorCache() flatMap {
        case false => Future(getGlobalErrorPage)
        case _ =>
          ersUtil.shortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](ersUtil.getCacheId, "callback_data_key_csv") flatMap { callback =>
            val validationResults = processCsvService.processFiles(callback, scheme, readFileCsv)
            finaliseRequestAndRedirect(validationResults)
          }
      }
  }

  def finaliseRequestAndRedirect(validationResults: List[Future[Either[Throwable, Boolean]]])(
    implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Result] =
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
                       (implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    clearErrorCache() map {
      case false => getGlobalErrorPage(request, messages)
    }

    //These .get's are safe because the UploadedSuccessfully model is already validated as existing in the UpscanController
    ersUtil.shortLivedCache.fetchAndGetEntry[UploadedSuccessfully](ersUtil.getCacheId, "callback_data_key") flatMap { file =>
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
          _ <- ersUtil.cache[String](ersUtil.FORMAT_ERROR_CACHE, e.message)
          _ <- ersUtil.cache[Seq[String]](ersUtil.FORMAT_ERROR_CACHE_PARAMS, e.optionalParams)
          _ <- ersUtil.cache[Boolean](ersUtil.FORMAT_ERROR_EXTENDED_CACHE, e.needsExtendedInstructions)
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
    }
  }
}
