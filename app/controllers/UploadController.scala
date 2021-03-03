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

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import config.ApplicationConfig
import controllers.auth.{AuthAction, RequestWithOptionalEmpRef}
import javax.inject.{Inject, Singleton}
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, SheetErrors}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{CsvFileProcessor, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.{CsvParserUtil, ERSUtil}
import services.DataGenerator

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes.{Success => akkaOk}
import akka.stream.scaladsl.{Concat, Flow, Sink, Source}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, MediaRanges}
import akka.stream.{Graph, SinkShape}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.util.ByteString
import org.apache.commons.io.FilenameUtils
import services.validation.ErsValidator
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}

import scala.annotation.tailrec

@Singleton
class UploadController @Inject()(authAction: AuthAction,
                                 processODSService: ProcessODSService,
                                 csvFileProcessor: CsvFileProcessor,
                                 mcc: MessagesControllerComponents,
                                 parserUtil: CsvParserUtil,
                                 dataGenerator: DataGenerator,
                                 implicit val ersUtil: ERSUtil,
                                 implicit val appConfig: ApplicationConfig
                                )(implicit executionContext: ExecutionContext, actorSystem: ActorSystem)
  extends FrontendController(mcc) with I18nSupport {

  def downloadAsInputStream(downloadUrl: String): InputStream = new URL(downloadUrl).openStream()

  def clearCache()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Boolean] = {
    //remove function doesn't work, the cache needs to be overwritten with 'blank' data
    (for {
      _ <- ersUtil.cache[Long](ersUtil.SCHEME_ERROR_COUNT_CACHE, 0L)
      _ <- ersUtil.cache[ListBuffer[SheetErrors]](ersUtil.ERROR_LIST_CACHE, new ListBuffer[SheetErrors]())
    } yield {
      Logger.debug(s"[UploadController][clearCache] Successfully cleared cache")
      true
    }) recoverWith {
      case e: Exception =>
        Logger.error(s"[UploadController][clearCache] Failed to clear cache of errors and error count", e)
        Future.successful(false)
    }
  }

  private[controllers] def readFileCsv(downloadUrl: String): Source[Either[Throwable, List[ByteString]], _] = {

    def extractEntityData(response: HttpResponse): Source[ByteString, _] =
      response match {
        case HttpResponse(akka.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withoutSizeLimit().dataBytes
        case notOkResponse =>
          Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
      }

    Source
      .single(HttpRequest(uri = downloadUrl))
      .mapAsync(1)(Http()(actorSystem).singleRequest(_))
      .flatMapConcat(extractEntityData)
      .via(CsvParsing.lineScanner())
      .via(Flow.fromFunction(Right(_)))
      .recover {
        case e => Left(e)
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

    val contentInputStream: InputStream = findFileInZip(zipInputStream)
    new StaxProcessor(contentInputStream)
  }

  def uploadCSVFile(scheme: String): Action[AnyContent] = authAction.async {
    implicit request =>
      showUploadCSVFile(scheme)
  }

  @tailrec
  private def listWithFirstNEntriesZippedNameTBD(n: Int, list: Seq[(List[ValidationError], Int)]): Seq[(List[ValidationError], Int)] = {
    if (n == 0) list
    else {
      val indexOfFirstOccurrence: Int = list.indexWhere(entry => entry._1.nonEmpty && entry._1.exists(validationError => validationError.cell.row == 0))
      if (indexOfFirstOccurrence != -1) {
        val listOriginalReference = list(indexOfFirstOccurrence)._1
        val entryReplacement = (listOriginalReference.map(validationError => {
          val cellReplaced = validationError.cell.copy(row = indexOfFirstOccurrence + 1)
          validationError.copy(cell = cellReplaced)
        }), indexOfFirstOccurrence)
        Logger.info("replaced with " + entryReplacement)
        listWithFirstNEntriesZippedNameTBD(n - 1, list.updated(indexOfFirstOccurrence, entryReplacement))
      } else list
    }
  }

  def giveRowNumbers(list: Seq[List[ValidationError]]): Seq[List[ValidationError]] = {
    val numberOfErrorsToDisplay: Int = 20
    listWithFirstNEntriesZippedNameTBD(numberOfErrorsToDisplay, list.zipWithIndex).map(_._1)
  }

  def checkFileType(name: String)(implicit messages: Messages): Either[Throwable, String] = if (name.takeRight(4) == ".csv") {
    Right(FilenameUtils.removeExtension(name))
  } else {
    Left(ERSFileProcessingException(
      Messages("ers_check_csv_file.file_type_error", name)(messages),
      Messages("ers_check_csv_file.file_type_error", name)(messages)))
  }

  def eitherFromFunction[A, B](input: A => Either[Throwable, B]): Flow[Either[Throwable, A], Either[Throwable, B], NotUsed] =
    Flow.fromFunction(_.flatMap(input))

  implicit class FlowOps[B](flow: Source[Either[Throwable, B], _]) {
    def divertOrExtract(errorSink: Sink[Either[Throwable, B], Future[Done]]) = {
      flow
        .divertTo(errorSink, _.isLeft)
        .via(Flow.fromFunction(_.right.get))
    }
  }

  def showUploadCSVFile(scheme: String)(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier, messages: Messages): Future[Result] = {
    clearCache() map {
      case false => getGlobalErrorPage(request, messages)
    }

    ersUtil.shortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](ersUtil.getCacheId, "callback_data_key_csv") flatMap { callback =>

      val processFiles: List[Future[Try[Boolean]]] = callback.get.files map { file =>
        val successUpload = file.uploadStatus.asInstanceOf[UploadedSuccessfully]

        val validatorFuture: Future[Either[Throwable, DataValidator]] = Source(List(successUpload.name))
          .via(Flow.fromFunction(checkFileType(_)(messages)))
          .via(eitherFromFunction(dataGenerator.getSheetCsv(_, scheme)(messages)))
          .via(eitherFromFunction(dataGenerator.identifyAndDefineSheetEither(_)(hc, request, messages)))
          .via(eitherFromFunction(dataGenerator.setValidatorCsv(_)(hc, request, messages)))
          .runWith(Sink.head)

        validatorFuture.flatMap {either => either.fold(
          throwable => Future.successful(Failure(throwable)),
          validator => {
            val futureListOfErrors: Future[Seq[List[ValidationError]]] = readFileCsv(successUpload.downloadUrl)
              .divertOrExtract(Sink.foreach[Either[Throwable, List[ByteString]]](println))
              .via(Flow.fromFunction(csvFileProcessor.processRow(_, successUpload.name, ErsValidator.validateRow(validator))))
              .runWith(Sink.seq[List[ValidationError]])

            val isValidInput: Future[Try[Boolean]] = futureListOfErrors.flatMap { listOfRows =>
              if (listOfRows.isEmpty) {
                Logger.info("listOfRows is empty")
                Future.successful(Failure(ERSFileProcessingException(
                  messages("ers_check_csv_file.noData", successUpload.name),
                  messages("ers_check_csv_file.noData"),
                  needsExtendedInstructions = true)))
              } else {
                val rowsWithRowNumbers = giveRowNumbers(listOfRows)
                rowsWithRowNumbers.filter(rowErrors => rowErrors.nonEmpty) match {
                  case allGood if allGood.isEmpty => Future.successful(Success(true))
                  case errors =>
                    val errorsToCache = new ListBuffer[SheetErrors]() :+ parserUtil.getSheetErrors(SheetErrors(successUpload.name, rowsWithRowNumbers.filter(rowErrors => rowErrors.nonEmpty).flatten.to[ListBuffer]))
                    Logger.info("errorsToCache are " + errorsToCache.head.errors.length)
                    for {
                      _ <- ersUtil.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${file.uploadId.value}", errors.flatten.length)
                      _ <- ersUtil.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}${file.uploadId.value}",
                        errorsToCache)
                    } yield Success(false)
                }
              }
            }
            isValidInput
          }
          )
        }
      }

      Future.sequence(processFiles).map { list =>
        //Logger.info("list at the end is " + list)
        if (list.forall(_ == Success(true))) {
          Redirect(routes.CheckingServiceController.checkingSuccessPage())
        } else if (list.exists(_.isFailure)) {
          list.find(_.isFailure).head match {
            case Failure(exception) => {
              Logger.info("throwing exception right here! " + exception)
              throw exception
            }
          }
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
      case _ => throw t
    }
  }

  def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
    Ok(views.html.global_error(
      "ers.global_errors.title",
      "ers.global_errors.message")(request, messages, appConfig))
  }
}