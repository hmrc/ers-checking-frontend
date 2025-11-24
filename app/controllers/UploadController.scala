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
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalEmpRefAndPAYE}
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.apache.commons.io.FilenameUtils
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing
import org.apache.pekko.util.ByteString
import uk.gov.hmrc.validator.models.{ERSFileProcessingException, RowValidationResults, SheetErrors}
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.services.validation.models.ValidationError
import uk.gov.hmrc.validator.FlowOps.eitherFromFunction
import uk.gov.hmrc.validator.{ProcessCsvService, ProcessODSService}
import uk.gov.hmrc.validator.utils.CsvParserUtil
import utils.ERSUtil

import java.io.InputStream
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadController @Inject()(authAction: AuthAction,
                                 sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                 mcc: MessagesControllerComponents,
                                 override val global_error: views.html.global_error
                                )(implicit executionContext: ExecutionContext,
                                  actorSystem: ActorSystem, val ersUtil: ERSUtil, override val appConfig: ApplicationConfig)
  extends FrontendController(mcc) with I18nSupport with ErsBaseController with Logging {

  private val uploadCsvSizeLimit: Int = appConfig.upscanFileSizeLimit

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
          sessionCacheService.fetch[UpscanCsvFilesCallbackList](ersUtil.CALLBACK_DATA_KEY_CSV) flatMap { callback: Option[UpscanCsvFilesCallbackList] =>
//            val successfullyUploadedFiles: Seq[UploadedSuccessfully] = callback.get.files.map(_.uploadStatus.asInstanceOf[UploadedSuccessfully]) // TODO: COME BACK AND REMOVE GET...
//            val validatorFuture: Future[Either[Throwable, DataValidator]] = Source(List(successUpload.name))
//              .via(Flow.fromFunction(checkFileType(_)(messages)))
//              .via(eitherFromFunction(DataGenerator.getSheetCsv(_, scheme)(messages)))
//              .via(eitherFromFunction(DataGenerator.identifyAndDefineSheetCsv(_)(hc, messages)))
//              .via(eitherFromFunction(DataGenerator.setValidatorCsv(_)(hc, messages)))
//              .runWith(Sink.head)
            val validationResults = processFiles(callback, scheme, readFileCsv)
            finaliseRequestAndRedirect(validationResults)
          }
      }
  }

  def processFiles(callback: Option[UpscanCsvFilesCallbackList], scheme: String, source: String => Source[HttpResponse, _])
                  (implicit messages: Messages, request: Request[AnyContent]): List[Future[Either[Throwable, Boolean]]] =
    callback.get.files map { file: UpscanCsvFilesCallback =>
      val successUpload: UploadedSuccessfully = file.uploadStatus.asInstanceOf[UploadedSuccessfully]
      ProcessCsvService.getValidator(successUpload.name, scheme)(messages) match {
        case Left(validatorError: Throwable) => Future.successful(Left(validatorError))
        case Right(validator) =>
          val futureListOfErrors: Future[Seq[Either[Throwable, RowValidationResults]]] =
            extractBodyOfRequest(source(successUpload.downloadUrl))
              .via(eitherFromFunction(ProcessCsvService.processRow(_, successUpload.name, validator)))
              .runWith(Sink.seq)

          futureListOfErrors.map {
            getRowsWithNumbers(_, successUpload.name)(messages)
          }.flatMap{
            case Right(errorsFromRow) => checkValidityOfRows(errorsFromRow, successUpload.name, file)
            case Left(exception) => Future(Left(exception))
          }
      }
    }

  def checkValidityOfRows(listOfErrors: Seq[List[ValidationError]], name: String, file: UpscanCsvFilesCallback)(
    implicit request: Request[_]): Future[Either[Throwable, Boolean]] = {
    listOfErrors.filter(rowErrors => rowErrors.nonEmpty) match {
      case allGood if allGood.isEmpty => Future.successful(Right(true))
      case errors =>
        val errorsToCache = ListBuffer(CsvParserUtil.getSheetErrors(SheetErrors(FilenameUtils.removeExtension(name), errors.flatten.to(ListBuffer)))) // TODO: COME BACK TO...
        for {
          _ <- sessionCacheService.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${file.uploadId.value}", errors.flatten.length)
          _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}${file.uploadId.value}",
            errorsToCache)
        } yield Right(false)
    }
  }

  def getRowsWithNumbers(listOfErrors: Seq[Either[Throwable, RowValidationResults]], name: String)(
    implicit messages: Messages): Either[Throwable, Seq[List[ValidationError]]] = listOfErrors match {
    case allEmpty if allEmpty.isEmpty || allEmpty.filter(_.isRight).forall(_.map(_.rowWasEmpty).forall(identity)) =>
      Left(ERSFileProcessingException(
        "ers_check_csv_file.noData",
        messages("ers_check_csv_file.noData", name),
        needsExtendedInstructions = true,
        optionalParams = Seq(name)))
    case nonEmpty =>
      nonEmpty.find(_.isLeft) match {
        case Some(Left(issues)) => Left(issues)
        case _ =>
          val maybeErrors = nonEmpty.map(_.getOrElse(RowValidationResults(List())).validationErrors)
          Right(giveRowNumbers(maybeErrors))
      }
  }

  def giveRowNumbers(list: Seq[List[ValidationError]]): Seq[List[ValidationError]] = {
    val maximumNumberOfErrorsToDisplay: Int = appConfig.errorCount
    processDisplayedErrors(maximumNumberOfErrorsToDisplay, list.zipWithIndex).map(_._1)
  }

  @tailrec
  final def processDisplayedErrors(errorsLeftToDisplay: Int,
                                   rowsWithIndex: Seq[(List[ValidationError], Int)]): Seq[(List[ValidationError], Int)] = {
    if (errorsLeftToDisplay <= 0) {
      rowsWithIndex
    }
    else {
      val indexOfFirstOccurrence: Int = rowsWithIndex.indexWhere(errorsWithIndex => errorsWithIndex._1.nonEmpty &&
        errorsWithIndex._1.exists(validationError => validationError.cell.row == 0))
      if (indexOfFirstOccurrence != -1) {
        val listOriginalReference = rowsWithIndex(indexOfFirstOccurrence)._1
        val entryReplacement = (listOriginalReference.map(validationError => {
          val cellReplaced = validationError.cell.copy(row = indexOfFirstOccurrence + 1)
          validationError.copy(cell = cellReplaced)
        }), indexOfFirstOccurrence)
        processDisplayedErrors(errorsLeftToDisplay - entryReplacement._1.length, rowsWithIndex.updated(indexOfFirstOccurrence, entryReplacement))
      } else {
        rowsWithIndex
      }
    }
  }

  def extractBodyOfRequest: Source[HttpResponse, _] => Source[Either[Throwable, List[ByteString]], _] =
    _.flatMapConcat(extractEntityData)
      .via(CsvParsing.lineScanner())
      .via(Flow.fromFunction(Right(_)))
      .recover {
        case e => Left(e)
      }

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(org.apache.pekko.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadCsvSizeLimit).dataBytes
      case notOkResponse =>
        Source.failed(
          UpstreamErrorResponse(
            s"[ProcessCsvService][extractEntityData] Illegal response from Upscan: ${notOkResponse.status.intValue}, body: ${notOkResponse.entity.dataBytes}",
            notOkResponse.status.intValue))
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
            (processor: InputStream) => {
              val fileName = file.get.name
              sessionCacheService.cache[String](ersUtil.FILE_NAME_CACHE, fileName).recover { // TODO: COME BACK TO.....
                case e: Exception =>
                  logger.error("[ProcessODSService][performODSUpload] Unable to save File Name. Error: " + e.getMessage)
                  throw e
              }
              ProcessODSService.validateODSFile(fileName, processor)(scheme, messages) match {
                case Left(_) => // TODO: What do we want to do with this error?
                  Future.successful(Redirect(routes.HtmlReportController.htmlErrorReportPage(false)))
                case Right(value) =>
                  if (value) {
                    Future.successful(Redirect(routes.CheckingServiceController.checkingSuccessPage()))
                  }
                  else {
                    ??? // WHERE DO WE REDIRECT IF WE GET AN INVALID FILE?
                  }
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
