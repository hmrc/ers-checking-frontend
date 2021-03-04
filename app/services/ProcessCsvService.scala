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
import models.{ERSFileProcessingException, SheetErrors}
import org.apache.commons.io.FilenameUtils
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import services.FlowOps.eitherFromFunction
import services.validation.ErsValidator.getCells
import services.validation.ValidationContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.services.validation.{DataValidator, Row, ValidationError}
import utils.{CsvParserUtil, ERSUtil}

import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ProcessCsvService @Inject()(parserUtil: CsvParserUtil,
                                  dataGenerator: DataGenerator,
                                  appConfig: ApplicationConfig
                                 )(implicit executionContext: ExecutionContext,
                                   ersUtil: ERSUtil,
                                   actorSystem: ActorSystem) {

  type RowValidator = Seq[String] => Either[Throwable, Option[List[ValidationError]]]

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(akka.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withoutSizeLimit().dataBytes // TODO investigate size limit
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
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
            val futureListOfErrors: Future[Seq[Either[Throwable, List[ValidationError]]]] = extractBodyOfRequest(source(successUpload.downloadUrl))
              .via(eitherFromFunction(processRow(_, successUpload.name, validator)))
              .runWith(Sink.seq[Either[Throwable, List[ValidationError]]])

            futureListOfErrors.map {
              getRowsWithNumbers(_, successUpload.name)(messages)
            }.flatMap{
              case Right(thing) => checkValidityOfRows(thing, successUpload.name, file)
            }
          }
        )
      }
    }


  def processRow(rowBytes: List[ByteString], sheetName: String, validator: DataValidator): Either[Throwable, List[ValidationError]] = {
    val rowStrings = rowBytes.map(byteString => byteString.utf8String)
    val parsedRow = parserUtil.formatDataToValidate(rowStrings, sheetName)
    Try {
      validator.validateRow(Row(0, getCells(parsedRow, 0)), Some(ValidationContext))
    } match {
      case Failure(e) =>
        Logger.warn(e.toString)
        Left(e)
      case Success(list) => Right(list.getOrElse(List.empty))
    }
  }

  def checkFileType(name: String)(implicit messages: Messages): Either[Throwable, String] = if (name.endsWith(".csv")) {
    Right(FilenameUtils.removeExtension(name))
  } else {
    Left(ERSFileProcessingException(
      Messages("ers_check_csv_file.file_type_error", name)(messages),
      Messages("ers_check_csv_file.file_type_error", name)(messages)))
  }

// TODO Either re-write this or add comment to explain
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
        listWithFirstNEntriesZippedNameTBD(n - 1, list.updated(indexOfFirstOccurrence, entryReplacement))
      } else list
    }
  }

  def giveRowNumbers(list: Seq[List[ValidationError]]): Seq[List[ValidationError]] = {
    val numberOfErrorsToDisplay: Int = appConfig.errorCount.getOrElse(20)
    listWithFirstNEntriesZippedNameTBD(numberOfErrorsToDisplay, list.zipWithIndex).map(_._1)
  }

  def getRowsWithNumbers(listOfErrors: Seq[Either[Throwable, List[ValidationError]]], name: String)(
    implicit messages: Messages): Either[Throwable, Seq[List[ValidationError]]] = listOfErrors match {
    case allEmpty if allEmpty.forall(_.isRight) && allEmpty.forall(_.right.getOrElse(List.empty).isEmpty) =>
      Left(ERSFileProcessingException(
        messages("ers_check_csv_file.noData", name),
        messages("ers_check_csv_file.noData"),
        needsExtendedInstructions = true))
    case nonEmpty if nonEmpty.forall(_.isRight) =>
      Right(giveRowNumbers(nonEmpty.map(_.right.get)))
    case lefts => Left(lefts.find(_.isLeft).head.left.get)
  }

  def checkValidityOfRows(listOfErrors: Seq[List[ValidationError]], name: String, file: UpscanCsvFilesCallback)(
    implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Either[Throwable, Boolean]] = {
    listOfErrors.filter(rowErrors => rowErrors.nonEmpty) match {
      case allGood if allGood.isEmpty => Future.successful(Right(true))
      case errors =>
        val errorsToCache = new ListBuffer[SheetErrors]() :+ parserUtil.getSheetErrors(SheetErrors(name, errors.flatten.to[ListBuffer]))
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
