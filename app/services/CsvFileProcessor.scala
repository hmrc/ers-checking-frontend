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

import config.ApplicationConfig
import controllers.auth.RequestWithOptionalEmpRef
import javax.inject.{Inject, Singleton}
import models.upscan.UpscanCsvFilesCallback
import models.{ERSFileProcessingException, SheetErrors}
import org.apache.commons.io.FilenameUtils
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import services.validation.ErsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}
import utils.{CsvParserUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class CsvFileProcessor @Inject()(dataGenerator: DataGenerator,
                                 parserUtil: CsvParserUtil,
                                 uploadedFileUtil: UploadedFileUtil,
                                 appConfig: ApplicationConfig
                                )(implicit executionContext: ExecutionContext) {

  type RowValidator = (Seq[String], Int) => Option[List[ValidationError]]

  val converter: String => Array[String] = _.split(",")
  val defaultChunkSize: Int = 10000

//  def flowValidateCsvFile(helperClass: HelperClass(csv: (String, String), filename: String, scheme: String)): Either[HelperClass, ERSFileProcessingException] = {
//    if (helperClass.filename.takeRight(3) != "csv") {
//      val filenameWithoutExtension = FilenameUtils.removeExtension(filename)
//      Left(helperClass.copy(filename = filenameWithoutExtension))
//    } else {
//      Right(ERSFileProcessingException(...))
//    }
//  }

  def validateFile(rowContents: Map[String, String], sheetName: String, validator: RowValidator)(implicit messages: Messages): List[ValidationError] = {
    val start = System.currentTimeMillis()
    val chunkSize = appConfig.chunkSize.getOrElse(defaultChunkSize)
    val cpus = Runtime.getRuntime.availableProcessors()

    Logger.info(s"[CsvFileProcessor][validateFile] Validating file $sheetName cpus: $cpus chunkSize: $chunkSize")

    val errors = processRow(rowContents.values.toList, sheetName, validator)
    val timeTaken = System.currentTimeMillis() - start
    Logger.info(s"[CsvFileProcessor][validateFile] Validation of file $sheetName completed in $timeTaken ms")

    errors
  }

  def checkRowsExist(rowsWithData: Int, sheetName: String)(implicit messages: Messages): Try[Boolean] = {
    if(rowsWithData > 0) {
      Success(true)
    }
    else {
      Failure(ERSFileProcessingException(
        messages("ers_check_csv_file.noData", sheetName + ".csv"),
        messages("ers_check_csv_file.noData"),
        needsExtendedInstructions = true))
    }
  }

  def getRowsFromFile(fileIterator: Iterator[String]): (List[List[String]], Int) = {
    try {
      val rows = new ListBuffer[List[String]]
      var rowsWithData = 0

      while (fileIterator.hasNext) {
        val row = fileIterator.next().split(",").toList
        rows += row

        if (!dataGenerator.isBlankRow(row)) {
          rowsWithData += 1
        }
      }
      (rows.toList, rowsWithData)
    }
  }

  def numberOfChunks(rows: Int, chunkSize: Int): Int = {
    val chunks: Int = (rows / chunkSize) + (if (rows % chunkSize == 0) 0 else 1)
    chunks
  }

  def processRow(row: List[String], sheetName: String, validator: RowValidator): List[ValidationError] = {
      val parsedRow = parserUtil.formatDataToValidate(row, sheetName)
      validator(parsedRow, 0) match {
        case Some(newErrors) if newErrors.nonEmpty =>
          Logger.debug("[CsvFileProcessor][processChunk] schemeErrors size is " + newErrors.size)
          newErrors
        case _ => List.empty
      }
  }

  /*
  def checkResult(result: Future[List[ValidationError]], sheetName: String)(implicit messages: Messages): Try[List[ValidationError]] = {
    Await.ready(result, Duration.Inf)
    result.value match {
      case Some(Success(errors)) => Success(errors)
      case Some(Failure(ex: ERSFileProcessingException)) =>  Failure(ex)
      case _ =>
        Logger.error(s"[CsvFileProcessor][checkResult] Unexpected failure checking expected list of errors - ${result.value}")
        Failure(ERSFileProcessingException(
          messages("ers.exceptions.dataParser.fileParsingError", sheetName) ,
          messages("ers.exceptions.dataParser.parsingOfFileData")))
    }
  }
   */

}
