/*
 * Copyright 2020 HM Revenue & Customs
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
import utils.{ParserUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class CsvFileProcessor @Inject()(dataGenerator: DataGenerator,
                                 parserUtil: ParserUtil,
                                 uploadedFileUtil: UploadedFileUtil,
                                 appConfig: ApplicationConfig
                                )(implicit executionContext: ExecutionContext) {

  type RowValidator = (Seq[String], Int) => Option[List[ValidationError]]

  val converter: String => Array[String] = _.split(",")
  val defaultChunkSize: Int = 10000

  def readCSVFile(filename:String, file: Iterator[String], scheme:String)
								 (implicit request: Request[AnyContent], hc : HeaderCarrier, messages: Messages): SheetErrors = {
    Logger.debug("[CsvFileProcessor][readCSVFile] file.getName" + filename)
    val sheetName = dataGenerator.identifyAndDefineSheet(filename, scheme)

    implicit val validator: DataValidator = dataGenerator.setValidator(sheetName)
    Logger.debug("[CsvFileProcessor][readCSVFile] validator set " + validator.toString)
    val errors = new ListBuffer() ++= validateFile(file, sheetName, ErsValidator.validateRow(validator))
    SheetErrors(sheetName, errors)
  }

  def processCsvUpload(fileIterator: Iterator[String], filename: String, scheme:String, file: UpscanCsvFilesCallback)
											(implicit request: RequestWithOptionalEmpRef[AnyContent], hc : HeaderCarrier, messages: Messages): Future[Try[Boolean]] = {
		try {
      val errors = validateCsvFiles(fileIterator, filename, scheme)
      parserUtil.isFileValid(errors, Some(file))
    }
    catch {
      case e: ERSFileProcessingException => Future.successful(Failure(e))
    }
  }

  def validateCsvFiles(file: Iterator[String], filename: String, scheme:String)
											(implicit request: RequestWithOptionalEmpRef[AnyContent], hc : HeaderCarrier, messages: Messages): ListBuffer[SheetErrors] = {
    val filesErrors: ListBuffer[SheetErrors] = new ListBuffer()
		checkFileType(filename)
		val filenameUtil = FilenameUtils.removeExtension(filename)
		filesErrors += readCSVFile(filenameUtil, file, scheme)
    filesErrors
  }

  def checkFileType(filename: String)(implicit messages: Messages): Unit = {
    if (!uploadedFileUtil.checkCSVFileType(filename)) {
      throw ERSFileProcessingException(
        Messages("ers_check_csv_file.file_type_error", filename),
        Messages("ers_check_csv_file.file_type_error", filename))
    }
  }

  def validateFile(file: Iterator[String], sheetName:String, validator: RowValidator)(implicit messages: Messages): List[ValidationError]= {
    val start = System.currentTimeMillis()
    val chunkSize = appConfig.chunkSize.getOrElse(defaultChunkSize)
    val cpus = Runtime.getRuntime.availableProcessors()

    Logger.info(s"[CsvFileProcessor][validateFile] Validating file $sheetName cpus: $cpus chunkSize: $chunkSize")

    try {
      val (rows, rowsWithData) = getRowsFromFile(file)
      checkRowsExist(rowsWithData, sheetName) match {
        case Failure(ex) => throw ex
        case _ =>
      }

      val chunks = numberOfChunks(rows.size, chunkSize)

      val submissions = submitChunks(rows, chunks, chunkSize, sheetName, validator)
      val result = getResult(submissions)

      val errors = checkResult(result, sheetName) match {
        case Success(err) => err
        case Failure(ex) => throw ex
      }

      val timeTaken = System.currentTimeMillis() - start
      Logger.info(s"[CsvFileProcessor][validateFile] Validation of file $sheetName completed in $timeTaken ms")

      errors
    } catch {
      case e: Exception =>
        Logger.error (s"[CsvFileProcessor][validateFile] Exception thrown inside 'try'", e)
        throw e
    }
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

  def submitChunks(
      rows: List[List[String]],
      chunks: Int,
      chunkSize: Int,
      sheetName: String,
      validator: RowValidator): Array[Future[List[ValidationError]]] = {

    val futures = new Array[Future[List[ValidationError]]](chunks)

    for (chunk <- 1 to chunks) {
      val chunkStart = (chunk - 1) * chunkSize + 1
      val chunkEnd = (chunk * chunkSize).min(rows.size)

      futures(chunk - 1) = Future {
        val chunk = rows.slice(chunkStart - 1, chunkEnd)
        processChunk(chunk, chunkStart, sheetName, validator)
      }
    }
    futures
  }

  def processChunk(chunk: List[List[String]], chunkStart: Int, sheetName: String, validator: RowValidator): List[ValidationError] = {
    val errors: ListBuffer[ValidationError] = new ListBuffer()
    var rowNo = chunkStart
    chunk.foreach(row => {
      val parsedRow = parserUtil.formatDataToValidate(row, sheetName)
      validator(parsedRow, rowNo) match {
        case Some(newErrors) if newErrors.nonEmpty =>
          Logger.debug("[CsvFileProcessor][processChunk] schemeErrors size is " + errors.size)
          errors ++= newErrors
        case _ =>
      }
      rowNo += 1
    })
    errors.toList
  }

  def getResult(submissions: Array[Future[List[ValidationError]]]): Future[List[ValidationError]] = {
    val errors: ListBuffer[ValidationError] = new ListBuffer()

    val result = Future.fold(submissions)(errors)((a, b) => b match {
      case newErrors if newErrors.nonEmpty => a ++= newErrors
      case _ => a
    }).map(errors => errors.toList)

    result
  }

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

}
