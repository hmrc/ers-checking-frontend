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

package services

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import config.ApplicationConfig
import models.upscan.{UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import models.ERSFileProcessingException
import models.SheetErrors.format
import org.apache.commons.io.FilenameUtils
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import repository.ErsCheckingFrontendSessionCacheRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.validator.csv.CsvValidator
import uk.gov.hmrc.validator.models.csv.RowValidationResults
import uk.gov.hmrc.validator.models.ods.SheetErrors
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import utils.ERSUtil

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import cats.syntax.all._
import uk.gov.hmrc.validator.validation.allTemplates

@Singleton
class ProcessCsvService @Inject()(appConfig: ApplicationConfig,
                                  sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                  ersUtil: ERSUtil
                                 )(implicit executionContext: ExecutionContext,
                                   actorSystem: ActorSystem) extends Logging {

  private val uploadCsvSizeLimit: Int = appConfig.upscanFileSizeLimit

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(org.apache.pekko.http.scaladsl.model.StatusCodes.OK, _, entity, _) => entity.withSizeLimit(uploadCsvSizeLimit).dataBytes
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


  def processFiles(callback: Option[UpscanCsvFilesCallbackList],
                   scheme: String,
                   source: String => Source[HttpResponse, _])
                  (implicit request: Request[_], messages: Messages): List[Future[Either[Throwable, Boolean]]] = {
    logger.info(s"[ProcessCsvService][processFiles] callback $callback scheme: $scheme")

    callback.get.files map { file =>

      val successUpload: UploadedSuccessfully = file.uploadStatus.asInstanceOf[UploadedSuccessfully]
      val eitherFileNameOrError: Either[Throwable, String] = checkFileType(successUpload.name)
      eitherFileNameOrError match {
        case Left(fileProcessingException: ERSFileProcessingException) =>
          logger.info("[ProcessCsvService][processFiles] Failed to remove extension from file correctly")
          throw fileProcessingException
        case Right(successfulUploadName: String) =>
          val futureListOfErrors: Future[Seq[Either[Throwable, RowValidationResults]]] =
            extractBodyOfRequest(
              source(successUpload.downloadUrl)
            )
              .via(
                FlowOps.eitherFromFunction(
                  CsvValidator
                    .setValidatorAndValidateCsvRow(
                      allTemplates,
                      _,
                      successfulUploadName,
                      scheme.toUpperCase
                    )
                )
              )
              .runWith(Sink.seq[Either[Throwable, RowValidationResults]])
          futureListOfErrors.map {
            getRowsWithNumbers(_, successfulUploadName)(messages)
          }.flatMap {
            case Right(errorsFromRow: Seq[ValidationError]) => checkValidityOfRows(errorsFromRow, successfulUploadName, file)
            case Left(exception: Throwable) => Future(Left(exception))
          }
      }
    }
  }

  def getRowsWithNumbers(listOfErrors: Seq[Either[Throwable, RowValidationResults]], name: String)(
    implicit messages: Messages): Either[Throwable, Seq[ValidationError]] = {
    // NOTE: This traverse lets us go from a Seq[Either[Throwable, RowValidationResults]] to an Either[Throwable, Seq[RowValidationResults]]
    listOfErrors
      .traverse(identity)
      .flatMap { validationResults: Seq[RowValidationResults] =>
        val allRowsEmpty: Boolean = validationResults.forall(_.rowWasEmpty)
        // The file is empty if:
        // validationResults.isEmpty -> there was no data to validate
        // allRowsEmpty -> there were rows in the csv but they contain no data to validate
        if (validationResults.isEmpty || allRowsEmpty){
          Left(
            ERSFileProcessingException(
              "ers_check_csv_file.noData",
              messages("ers_check_csv_file.noData", name),
              needsExtendedInstructions = true,
              optionalParams = Seq(name)
            )
          )
        } else {
          val validationErrors: Seq[ValidationError] = validationResults
            .flatMap(
              _.validationErrors.map((error: ValidationError) => {
                val updatedCell: Cell = error.cell.copy(
                  row = error.cell.row + 1 // The original row number starts at 0, to map to the row in the csv we need to add 1
                )
                error.copy(cell = updatedCell)
              }
              )
            )
          Right(validationErrors.take(appConfig.errorCount))
        }
    }
  }

  def checkValidityOfRows(listOfErrors: Seq[ValidationError], name: String, file: UpscanCsvFilesCallback)(
    implicit request: Request[_]): Future[Either[Throwable, Boolean]] = {
    if (listOfErrors.isEmpty){
      Future.successful(Right(true))
    } else {
      val errorsToCache = ListBuffer(getSheetErrors(SheetErrors(FilenameUtils.removeExtension(name), listOfErrors.to(ListBuffer))))
          for {
            _ <- sessionCacheService.cache[Long](s"${ersUtil.SCHEME_ERROR_COUNT_CACHE}${file.uploadId.value}", listOfErrors.length)
            _ <- sessionCacheService.cache[ListBuffer[SheetErrors]](s"${ersUtil.ERROR_LIST_CACHE}${file.uploadId.value}",
              errorsToCache)
          } yield Right(false)
      }
    }

  def getSheetErrors(schemeErrors: SheetErrors): SheetErrors = {
    schemeErrors.copy(errors = schemeErrors.errors.take(appConfig.errorCount))
  }

  def checkFileType(name: String)(implicit messages: Messages): Either[Throwable, String] = if (name.endsWith(".csv")) {
    Right(FilenameUtils.removeExtension(name))
  } else {
    Left(ERSFileProcessingException(
      Messages("ers_check_csv_file.file_type_error", name)(messages),
      Messages("ers_check_csv_file.file_type_error", name)(messages)))
  }

  private object FlowOps {

    def eitherFromFunction[A, B](input: A => Either[Throwable, B]): Flow[Either[Throwable, A], Either[Throwable, B], _] =
      Flow.fromFunction(_.flatMap(input))

  }
}
