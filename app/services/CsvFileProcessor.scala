/*
 * Copyright 2016 HM Revenue & Customs
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
import org.apache.commons.io.{FileUtils, LineIterator}
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import services.validation.ErsValidator
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}
import utils.{ParserUtil, CacheUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

object CsvFileProcessor extends CsvFileProcessor
{
  override val cacheUtil:CacheUtil = CacheUtil
}


trait CsvFileProcessor extends DataGenerator {
  val cacheUtil:CacheUtil

  val converter: (String) => Array[String] = _.split(",")

  def readCSVFile(filename:String,file: File,scheme:String)(implicit request: Request[AnyContent], hc : HeaderCarrier) = {
    Logger.debug("file.getName" + filename)
    val sheetName = identifyAndDefineSheet(filename,scheme )

    implicit val validator = setValidator(sheetName)
    Logger.debug("validator set " + validator.toString)
    SheetErrors(sheetName, validateFile(file:File, sheetName, ErsValidator.validateRow)(validator, applicationMessages))

  }

  def processCsvUpload(scheme:String)(implicit request: Request[AnyContent], authContext : AuthContext, hc : HeaderCarrier) =
  {
    val errors = validateCsvFiles(scheme)
    ParserUtil.isFileValid(errors, "performCSVUpload")
  }

  def validateCsvFiles(scheme:String)(implicit request: Request[AnyContent], authContext : AuthContext, hc : HeaderCarrier) = {
   val files = request.body.asMultipartFormData.get.files
    val filesErrors: ListBuffer[SheetErrors] = new ListBuffer()
    files.map(file => {
          checkFileType(file.filename)
          filesErrors += readCSVFile(file.filename.dropRight(4),file.ref.file,scheme)
    })
      filesErrors
  }

  def checkFileType(filename:String) = {
    if (!UploadedFileUtil.checkCSVFileType(filename)) {
      throw ERSFileProcessingException(Messages("ers_check_csv_file.file_type_error", filename), Messages("ers_check_csv_file.file_type_error", filename))
    }
  }

  def validateFile(file:File, sheetName:String, validator:(Seq[String],Int,DataValidator) => Option[List[ValidationError]])(implicit dataValidator:DataValidator, messages: Messages): ListBuffer[ValidationError]= {
    val iterator:LineIterator = FileUtils.lineIterator(file, "UTF-8")

    var rowCount = 0
    val errorsList: ListBuffer[ValidationError] = new ListBuffer()

    try {
      var rowsWithData = 0
      while (iterator.hasNext) {
        rowCount = rowCount+1
        val rowData: Array[String] = iterator.nextLine().split(",")
        if(!isBlankRow(rowData)){
          rowsWithData += 1
          val dataToValidate = ParserUtil.formatDataToValidate(rowData, sheetName)
          Logger.debug("Row Num :- "+ rowCount +  " -- Data retrieved:-" + dataToValidate.mkString)
          validator(dataToValidate, rowCount, dataValidator) match {
            case errors:Option[List[ValidationError]] if errors.isDefined => {
            //  Logger.debug("Error while Validating File + Formatting errors present " + errors.toString)
              Logger.debug("schemeErrors size is " + errors.size)
              errorsList ++= errors.get
            }
            case _ => Logger.debug("No Errors for "+ "row "+ rowCount)
          }
        }
      }
      if(rowsWithData == 0) {
        throw ERSFileProcessingException(messages("ers_check_csv_file.noData", sheetName + ".csv"), messages("ers_check_csv_file.noData"), needsExtendedInstructions = true)
      }
      errorsList
    }
    catch {
      case ex: ERSFileProcessingException => {
        throw ex
      }
      case ex:Throwable => {
        UploadedFileUtil.deleteFile(file: File)
        throw new ERSFileProcessingException(messages("ers.exceptions.dataParser.fileParsingError", sheetName) ,messages("ers.exceptions.dataParser.parsingOfFileData"))
      }
    }
    finally {
      LineIterator.closeQuietly(iterator)
      UploadedFileUtil.deleteFile(file: File)
    }
  }

}
