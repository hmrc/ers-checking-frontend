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

import cats.implicits.toTraverseOps
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
import uk.gov.hmrc.validator._
import cats.data.EitherT

import scala.util.{Failure, Success}

@Singleton
class ProcessCsvService @Inject()(appConfig: ApplicationConfig,
                                  sessionCacheService: ErsCheckingFrontendSessionCacheRepository,
                                  ersUtil: ERSUtil
                                 )(implicit executionContext: ExecutionContext,
                                   actorSystem: ActorSystem) extends Logging {

  private val uploadCsvSizeLimit: Int = appConfig.upscanFileSizeLimit

  implicit val futureMonad = cats.instances.future.catsStdInstancesForFuture(executionContext) // TODO: TIDY

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

  def validateCsv(
                   source: Source[HttpResponse, _],
                   dataEngine: DataEngine
                 ): Future[Either[Throwable, Seq[RowValidationResults]]] =
    extractBodyOfRequest(source)
      .via(FlowOps.eitherFromFunction(
        (rowBytes: Seq[ByteString]) => CsvValidator.validateCsvRow(dataEngine, rowBytes))
      )
      .runWith(Sink.seq[Either[Throwable, RowValidationResults]])
      .map(_.traverse(identity))


  def processFile(scheme: String, downloadSourceFile: String => Source[HttpResponse, _], file: UpscanCsvFilesCallback)
                 (implicit request: Request[_], messages: Messages): Future[Either[Throwable, Boolean]] = {

    val successUpload: UploadedSuccessfully = file.uploadStatus.asInstanceOf[UploadedSuccessfully]
    val source: Source[HttpResponse, _] = downloadSourceFile(successUpload.downloadUrl)

    (for {
      sheetName           <- EitherT.fromEither[Future](checkFileType(successUpload.name))
      _                   <- EitherT.fromEither[Future](ERSTemplatesInfo.findSheetWithinSchemeType(sheetName, scheme))
      dataEngine          <- EitherT.fromEither(DataEngine(sheetName, SchemeVersion.All))
      csvValidationResult <- EitherT(validateCsv(source, dataEngine))
      rowsWithNumbers     <- EitherT.fromEither(getValidationResultsWithCorrectRowNumber(csvValidationResult, sheetName)(messages))
      result              <- EitherT(checkValidityOfRows(rowsWithNumbers, sheetName, file))
    } yield result).value
  }

  def processFiles(callback: Option[UpscanCsvFilesCallbackList],
                   scheme: String,
                   downloadSourceFile: String => Source[HttpResponse, _])
                  (implicit request: Request[_], messages: Messages): List[Future[Either[Throwable, Boolean]]] = {
    logger.info(s"[ProcessCsvService][processFiles] callback $callback scheme: $scheme")
    callback.get.files map { file: UpscanCsvFilesCallback =>
      processFile(scheme = scheme, downloadSourceFile = downloadSourceFile, file = file)
    }
  }

  def generateNoDataException(name: String)(implicit messages: Messages): ERSFileProcessingException =
    ERSFileProcessingException(
      "ers_check_csv_file.noData",
      messages("ers_check_csv_file.noData", name),
      needsExtendedInstructions = true,
      optionalParams = Seq(name)
    )

  def checkIfValidationResultsEmpty(validationResults: Seq[RowValidationResults]): Boolean = {
    val allRowsEmpty: Boolean = validationResults.forall(_.rowWasEmpty)
    validationResults.isEmpty || allRowsEmpty
  }

  def getValidationResultsWithCorrectRowNumber(validationResults: Seq[RowValidationResults], name: String)(
    implicit messages: Messages): Either[Throwable, Seq[ValidationError]] = {
    if (checkIfValidationResultsEmpty(validationResults)){
      Left(generateNoDataException(name))
    } else {
      Right(
        updateValidationResultRowNumbers(validationResults)
          .take(appConfig.errorCount)
      )
    }
  }

  def updateValidationResultRowNumbers(validationResults: Seq[RowValidationResults]): Seq[ValidationError] =
    validationResults
      .flatMap(
        _.validationErrors.map((error: ValidationError) => {
          val updatedCell: Cell = error.cell.copy(
            row = error.cell.row + 1 // The original row number starts at 0, to map to the row in the csv we need to add 1
          )
          error.copy(cell = updatedCell)
        }
        )
      )

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

  def checkFileType(name: String)(implicit messages: Messages): Either[Throwable, String] =
    if (name.endsWith(".csv")) {
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
