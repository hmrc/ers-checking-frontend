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

package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import config.ApplicationConfig
import controllers.auth.RequestWithOptionalEmpRef
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, RowValidationResults, SheetErrors}
import org.apache.commons.io.FilenameUtils
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import services.FlowOps.eitherFromFunction
import services.validation.ErsValidator.getCells
import uk.gov.hmrc.services.validation.models._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.services.validation.DataValidator
import utils.{CsvParserUtil, ERSUtil}

import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ProcessCsvService @Inject()(parserUtil: CsvParserUtil,
                                  dataGenerator: DataGenerator,
                                  appConfig: ApplicationConfig,
                                  ersUtil: ERSUtil
                                 )(implicit executionContext: ExecutionContext,
                                   actorSystem: ActorSystem) {

  private val uploadCsvSizeLimit: Int = appConfig.uploadCsvSizeLimit

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(akka.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadCsvSizeLimit).dataBytes
      case notOkResponse =>
        Source.failed(
          UpstreamErrorResponse(
            s"[ProcessCsvService][extractEntityData] Illegal response from Upscan: ${notOkResponse.status.intValue}, body: ${notOkResponse.entity.dataBytes}",
            notOkResponse.status.intValue))
    }

  def extractBodyOfRequest: Source[HttpResponse, _] => Source[Either[Throwable, List[ByteString]], _] =
    _.flatMapConcat(extractEntityData)
      .via(CsvParsing.lineScanner())
      .via(Flow.fromFunction(Right(_)))
      .recover {
        case e => Left(e)
      }

  def processFiles(callback: Option[UpscanCsvFilesCallbackList], scheme: String, source: String => Source[HttpResponse, _])(
    implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier, messages: Messages
  ): List[Future[Either[Throwable, Boolean]]] =
    callback.get.files map { file =>

      val successUpload = file.uploadStatus.asInstanceOf[UploadedSuccessfully]

      val validatorFuture: Future[Either[Throwable, DataValidator]] = Source(List(successUpload.name))
        .via(Flow.fromFunction(checkFileType(_)(messages)))
        .via(eitherFromFunction(dataGenerator.getSheetCsv(_, scheme)(messages)))
        .via(eitherFromFunction(dataGenerator.identifyAndDefineSheetEither(_)(hc, request, messages)))
        .via(eitherFromFunction(dataGenerator.setValidatorCsv(_)(hc, request, messages)))
        .runWith(Sink.head)

      validatorFuture.flatMap {
        _.fold(
          throwable => Future.successful(Left(throwable)),
          validator => {
            val futureListOfErrors: Future[Seq[Either[Throwable, RowValidationResults]]] = extractBodyOfRequest(source(successUpload.downloadUrl))
              .via(eitherFromFunction(processRow(_, successUpload.name, validator)))
              .runWith(Sink.seq[Either[Throwable, RowValidationResults]])

            futureListOfErrors.map {
              getRowsWithNumbers(_, successUpload.name)(messages)
            }.flatMap{
              case Right(errorsFromRow) => checkValidityOfRows(errorsFromRow, successUpload.name, file)
              case Left(exception) => Future(Left(exception))
            }
          }
        )
      }
    }

  def processRow(rowBytes: List[ByteString], sheetName: String, validator: DataValidator): Either[Throwable, RowValidationResults] = {
    val rowStrings = rowBytes.map(byteString => byteString.utf8String)
    val parsedRow = parserUtil.formatDataToValidate(rowStrings, sheetName)
    val rowIsEmpty = parserUtil.rowIsEmpty(rowStrings)

    Try {
      validator.validateRow(Row(0, getCells(parsedRow, 0)))
    } match {
      case Failure(e) =>
        Logger.warn(e.toString)
        Left(e)
      case Success(list) => Right(RowValidationResults(list.getOrElse(List.empty), rowIsEmpty))
    }
  }

  def checkFileType(name: String)(implicit messages: Messages): Either[Throwable, String] = if (name.endsWith(".csv")) {
    Right(FilenameUtils.removeExtension(name))
  } else {
    Left(ERSFileProcessingException(
      Messages("ers_check_csv_file.file_type_error", name)(messages),
      Messages("ers_check_csv_file.file_type_error", name)(messages)))
  }

@tailrec
private[services] final def processDisplayedErrors(n: Int, list: Seq[(List[ValidationError], Int)]): Seq[(List[ValidationError], Int)] = {
  if (n == 0) list
  else {
    val indexOfFirstOccurrence: Int = list.indexWhere(entry => entry._1.nonEmpty && entry._1.exists(validationError => validationError.cell.row == 0))
    if (indexOfFirstOccurrence != -1) {
      val listOriginalReference = list(indexOfFirstOccurrence)._1
      val entryReplacement = (listOriginalReference.map(validationError => {
        val cellReplaced = validationError.cell.copy(row = indexOfFirstOccurrence + 1)
        validationError.copy(cell = cellReplaced)
      }), indexOfFirstOccurrence)
      processDisplayedErrors(n - 1, list.updated(indexOfFirstOccurrence, entryReplacement))
    } else list
  }
}

  def giveRowNumbers(list: Seq[List[ValidationError]]): Seq[List[ValidationError]] = {
    val numberOfErrorsToDisplay: Int = appConfig.errorCount
    processDisplayedErrors(numberOfErrorsToDisplay, list.zipWithIndex).map(_._1)
  }

  def getRowsWithNumbers(listOfErrors: Seq[Either[Throwable, RowValidationResults]], name: String)(
    implicit messages: Messages): Either[Throwable, Seq[List[ValidationError]]] = listOfErrors match {
    case allEmpty if allEmpty.isEmpty || allEmpty.filter(_.isRight).forall(_.right.get.rowWasEmpty) =>
      Left(ERSFileProcessingException(
        messages("ers_check_csv_file.noData", name),
        messages("ers_check_csv_file.noData"),
        needsExtendedInstructions = true))
    case nonEmpty =>
      nonEmpty.find(_.isLeft) match {
      case Some(Left(issues)) => Left(issues)
      case _ => Right(giveRowNumbers(nonEmpty.map(_.right.get.validationErrors)))
    }
  }

  def checkValidityOfRows(listOfErrors: Seq[List[ValidationError]], name: String, file: UpscanCsvFilesCallback)(
    implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Either[Throwable, Boolean]] = {
    listOfErrors.filter(rowErrors => rowErrors.nonEmpty) match {
      case allGood if allGood.isEmpty => Future.successful(Right(true))
      case errors =>
        val errorsToCache = ListBuffer(parserUtil.getSheetErrors(SheetErrors(FilenameUtils.removeExtension(name), errors.flatten.to[ListBuffer])))
        for {
          _ <- ersUtil.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${file.uploadId.value}", errors.flatten.length)
          _ <- ersUtil.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}${file.uploadId.value}",
            errorsToCache)
        } yield Right(false)
    }
  }
}


object FlowOps {

  def eitherFromFunction[A, B](input: A => Either[Throwable, B]): Flow[Either[Throwable, A], Either[Throwable, B], NotUsed] =
    Flow.fromFunction(_.flatMap(input))

}
