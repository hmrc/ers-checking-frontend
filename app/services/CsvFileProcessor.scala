/*
 * Copyright 2017 HM Revenue & Customs
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

import java.io.File

import models.{ERSFileProcessingException, SheetErrors}
import org.apache.commons.io.{FileUtils, FilenameUtils, LineIterator}
import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request}
import services.validation.ErsValidator
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}
import utils.{CacheUtil, ParserUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object CsvFileProcessor extends CsvFileProcessor {
  override val cacheUtil: CacheUtil = CacheUtil
}

trait CsvFileProcessor extends DataGenerator {
  val cacheUtil: CacheUtil

  type RowValidator = (Seq[String], Int) => Option[List[ValidationError]]

  val converter: (String) => Array[String] = _.split(",")

  def readCSVFile(filename:String,file: File,scheme:String)(implicit request: Request[AnyContent], hc : HeaderCarrier): SheetErrors = {
    Logger.debug("file.getName" + filename)
    val sheetName = identifyAndDefineSheet(filename,scheme )

    implicit val validator: DataValidator = setValidator(sheetName)
    Logger.debug("validator set " + validator.toString)
    val errors = new ListBuffer() ++= validateFile(file:File, sheetName, ErsValidator.validateRow(validator))
    SheetErrors(sheetName, errors)
  }

  def processCsvUpload(scheme:String)(implicit request: Request[AnyContent], authContext : AuthContext, hc : HeaderCarrier): Boolean =
  {
    val errors = validateCsvFiles(scheme)
    ParserUtil.isFileValid(errors, "performCSVUpload")
  }

  def validateCsvFiles(scheme:String)(implicit request: Request[AnyContent], authContext : AuthContext, hc : HeaderCarrier): ListBuffer[SheetErrors] = {
   val files = request.body.asMultipartFormData.get.files
    val filesErrors: ListBuffer[SheetErrors] = new ListBuffer()
    files.map(file => {
        if(!file.filename.isEmpty) {
          checkFileType(file.filename)
          val filename = FilenameUtils.removeExtension(file.filename)
          filesErrors += readCSVFile(filename, file.ref.file, scheme)
        }
    })
    filesErrors
  }

  def checkFileType(filename:String): Unit = {
    if (!UploadedFileUtil.checkCSVFileType(filename)) {
      throw ERSFileProcessingException(
        Messages("ers_check_csv_file.file_type_error", filename),
        Messages("ers_check_csv_file.file_type_error", filename))
    }
  }

  def validateFile(file:File, sheetName:String, validator: RowValidator): List[ValidationError]= {
    val start = System.currentTimeMillis()
    Logger.debug(s"Validating file ${file.getName}")

    try {
      val (rows, rowsWithData) = getRowsFromFile(file, sheetName)
      checkRowsExist(rowsWithData, sheetName) match {
        case Failure(ex) => throw ex
        case _ =>
      }

      val chunkSize = current.configuration.getInt("validationChunkSize").getOrElse(10000)
      val chunks = numberOfChunks(rows.size, chunkSize)

      val submissions = submitChunks(rows, chunks, chunkSize, sheetName, validator)
      val result = getResult(submissions)

      val errors = checkResult(result, sheetName) match {
        case Success(err) => err
        case Failure(ex) => throw ex
      }

      val timeTaken = System.currentTimeMillis() - start
      Logger.info(s"File validation completed in $timeTaken ms")

      errors
    }
    finally {
      UploadedFileUtil.deleteFile(file: File)
    }
  }

  def checkRowsExist(rowsWithData: Int, sheetName: String): Try[Boolean] = {
    if(rowsWithData > 0) {
      Success(true)
    }
    else {
      Failure(ERSFileProcessingException(
        Messages("ers_check_csv_file.noData", sheetName + ".csv"),
        Messages("ers_check_csv_file.noData"),
        needsExtendedInstructions = true))
    }
  }

  def getRowsFromFile(file: File, sheetName: String): (List[List[String]], Int) = {
    val iterator:LineIterator = FileUtils.lineIterator(file, "UTF-8")
    try {

      val rows = new ListBuffer[List[String]]
      var rowsWithData = 0

      while (iterator.hasNext) {
        val row = iterator.next().split(",").toList
        rows += row

        if (!isBlankRow(row)) {
          rowsWithData += 1
        }
      }

      (rows.toList, rowsWithData)
    }
    finally {
      LineIterator.closeQuietly(iterator)
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

  def processChunk(
      chunk: List[List[String]],
      chunkStart: Int,
      sheetName: String,
      validator: RowValidator): List[ValidationError] = {

    val errors: ListBuffer[ValidationError] = new ListBuffer()
    var rowNo = chunkStart
    chunk.foreach(row => {
      val parsedRow = ParserUtil.formatDataToValidate(row, sheetName)
      validator(parsedRow, rowNo) match {
        case Some(newErrors) if newErrors.nonEmpty =>
          Logger.debug("schemeErrors size is " + errors.size)
          errors ++= newErrors
        case _ => Logger.debug("No Errors for " + "row " + rowNo)
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

  def checkResult(result: Future[List[ValidationError]], sheetName: String): Try[List[ValidationError]] = {
    Await.ready(result, Duration.Inf)

    result.value match {
      case Some(Success(errors)) => Success(errors)
      case Some(Failure(ex: ERSFileProcessingException)) =>  Failure(ex)
      case _ =>
        Failure(ERSFileProcessingException(
          Messages("ers.exceptions.dataParser.fileParsingError", sheetName) ,
          Messages("ers.exceptions.dataParser.parsingOfFileData")))
    }
  }

}
