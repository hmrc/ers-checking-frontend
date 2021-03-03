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

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.scaladsl.Flow
import controllers.auth.RequestWithOptionalEmpRef
import javax.inject.{Inject, Singleton}
import metrics.Metrics
import models.{ERSFileProcessingException, SheetErrors}
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Request
import services.ERSTemplatesInfo.ersSheets
import services.audit.AuditEvents
import services.validation.ErsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.services.validation.DataValidator
import utils.{CsvParserUtil, ERSUtil, ParserUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class DataGenerator @Inject()(auditEvents: AuditEvents,
                              metrics: Metrics,
                              parserUtil: ParserUtil,
                              csvParserUtil: CsvParserUtil,
                              ersUtil: ERSUtil
                             )(implicit ec: ExecutionContext) extends DataParser {

  def getErrors(iterator:Iterator[String], scheme:String, fileName : String)
               (implicit hc: HeaderCarrier, request: RequestWithOptionalEmpRef[_], messages: Messages) =
  {
    var rowNum = 0
    implicit var sheetName :String = ""
    var sheetColSize = 0
    val schemeErrors: ListBuffer[SheetErrors] = ListBuffer()
    var validator:DataValidator = ERSValidationConfigs.defValidator

    def incRowNum(): Unit = rowNum =  rowNum + 1
    var rowCount : Int = 0
    var rowsWithData : Int = 0
    val startTime = System.currentTimeMillis()

    while(iterator.hasNext){
      val row = iterator.next()
      val rowData = parse(row, fileName)
      Logger.debug("[DataGenerator][getErrors] parsed data ---> " + rowData + " -- cursor --> " + rowNum)
      if (rowData.isLeft) {
        checkForMissingHeaders(rowNum, sheetName, fileName)
        Logger.debug("[DataGenerator][getErrors] data from the left --->" + rowData.left.get)
        sheetName = identifyAndDefineSheet(rowData.left.get, scheme)
        Logger.debug("[DataGenerator][getErrors] Sheetname = " + sheetName + "******")
        schemeErrors += SheetErrors(sheetName, ListBuffer())
        Logger.debug("[DataGenerator][getErrors] SchemeData = " + schemeErrors.size + "******")
        rowNum = 1
        validator = setValidator(sheetName)
      } else {
        for (i <- 1 to rowData.right.get._2) {
          rowNum match {
            case count if count < 9 =>
              Logger.debug("[DataGenerator][getErrors] GetData: incRowNum if count < 9: " + count + " RowNum: " + rowNum)
              incRowNum()
            case 9 =>
              Logger.debug("[DataGenerator][getErrors] GetData: incRowNum if  9: " + rowNum + "sheetColSize: " + sheetColSize)
              Logger.debug("[DataGenerator][getErrors] sheetName--->" + sheetName)
              sheetColSize = validateHeaderRow(rowData.right.get._1, sheetName, scheme, fileName)
              incRowNum()
            case _ =>
              val foundData = rowData.right.get._1
              rowCount = rowData.right.get._1.size
              val data = parserUtil.formatDataToValidate(foundData, sheetName)
              if (!isBlankRow(data)) {
                rowsWithData += 1
                ErsValidator.validateRow(validator)(data, rowNum) match {
                  case Some(errors) if errors.nonEmpty =>
                    Logger.debug("[DataGenerator][getErrors] Error while Validating File + Formatting errors present " + errors.toString)
                    schemeErrors.last.errors ++= errors
                  case _ => schemeErrors.last.errors
                }
              }
              incRowNum()
          }
        }
      }
    }

    checkForMissingHeaders(rowNum, sheetName, fileName)
    if(rowsWithData == 0) {
      throw ERSFileProcessingException("ers.exceptions.dataParser.noData", Messages("ers.exceptions.dataParser.noData"), needsExtendedInstructions = true)
    }
    deliverDataIteratorMetrics(startTime)
    auditEvents.numRowsInSchemeData(scheme, rowsWithData)(hc, request, ec)
    Logger.debug("[DataGenerator][getErrors] The SchemeData that GetData finally returns: " + schemeErrors)
    schemeErrors
  }

  def checkForMissingHeaders(rowNum: Int, sheetName: String, fileName: String)(implicit messages: Messages): Unit = {
    if(rowNum > 0 && rowNum < 9) {
      throw ERSFileProcessingException(
        "ers.exceptions.dataParser.incorrectHeader",
        Messages("ers.exceptions.dataParser.incorrectHeader", sheetName, fileName),
        needsExtendedInstructions = true,
        optionalParams = Seq(sheetName, fileName)
      )
    }
  }

  def setValidator(sheetName:String)(implicit hc : HeaderCarrier, request: Request[_], messages: Messages): DataValidator = {
    try {
      ERSValidationConfigs.getValidator(ersSheets(sheetName).configFileName)
    }catch{
      case e: Exception =>
        auditEvents.auditRunTimeError(e,"Could not set the validator", sheetName)(hc, request, ec)
        Logger.error("setValidator has thrown an exception, SheetName: " + sheetName + " Exception message: " + e.getMessage)
        throw ERSFileProcessingException(
          "ers.exceptions.dataParser.configFailure",
          Messages("ers.exceptions.dataParser.validatorError"),
          optionalParams = Seq(sheetName)
        )
    }
  }

  def setValidatorCsv(sheetName:String)(implicit hc : HeaderCarrier, request: Request[_], messages: Messages): Either[Throwable, DataValidator] = {
    Try {
      ERSValidationConfigs.getValidator(ersSheets(sheetName).configFileName)
    } match {
      case Success(validator) => Right(validator)
      case Failure(e) =>
        auditEvents.auditRunTimeError(e, "Could not set the validator", sheetName)(hc, request, ec)
        Logger.error("setValidator has thrown an exception, SheetName: " + sheetName + " Exception message: " + e.getMessage)
        Left(ERSFileProcessingException(
          "ers.exceptions.dataParser.configFailure",
          Messages("ers.exceptions.dataParser.validatorError"),
          optionalParams = Seq(sheetName)
        ))
    }
  }

  def identifyAndDefineSheet(filename: String, scheme: String)(implicit hc: HeaderCarrier, request: Request[_], messages: Messages): String = {
    Logger.debug("5.1  case 0 identifyAndDefineSheet  " )
    val sheetInfo = getSheet(filename, scheme)
    val schemeName = ersUtil.getSchemeName(scheme)._2
    if (sheetInfo.schemeType.toLowerCase == schemeName.toLowerCase) {
      Logger.debug("****5.1.1  data contains data:  *****" + filename)
      filename
    } else {
      auditEvents.fileProcessingErrorAudit(sheetInfo.schemeType, sheetInfo.sheetName, s"${sheetInfo.schemeType.toLowerCase} is not equal to ${schemeName.toLowerCase}")
      Logger.warn(Messages("ers.exceptions.dataParser.incorrectSchemeType", sheetInfo.schemeType.toUpperCase, schemeName.toUpperCase))
      throw ERSFileProcessingException("ers.exceptions.dataParser.incorrectSchemeType",
        Messages("ers.exceptions.dataParser.incorrectSchemeType", sheetInfo.schemeType.toLowerCase, schemeName.toLowerCase),
        optionalParams = Seq(ersUtil.withArticle(sheetInfo.schemeType.toUpperCase), ersUtil.withArticle(schemeName.toUpperCase), sheetInfo.sheetName))
    }
  }

   def identifyAndDefineSheetEither(informationOnInput: (SheetInfo, String))(implicit hc: HeaderCarrier, request: Request[_], messages: Messages): Either[Throwable, String] = {
     val sheetInfo = informationOnInput._1
     val schemeName = informationOnInput._2

     if (sheetInfo.schemeType.toLowerCase == schemeName.toLowerCase) {
       Logger.debug("****5.1.1  data contains data:  *****" + sheetInfo.sheetName)
       Right(sheetInfo.sheetName)
     } else {
       auditEvents.fileProcessingErrorAudit(sheetInfo.schemeType, sheetInfo.sheetName, s"${sheetInfo.schemeType.toLowerCase} is not equal to ${schemeName.toLowerCase}")
       Logger.warn(Messages("ers.exceptions.dataParser.incorrectSchemeType", sheetInfo.schemeType.toUpperCase, schemeName.toUpperCase))
       Left(ERSFileProcessingException("ers.exceptions.dataParser.incorrectSchemeType",
         Messages("ers.exceptions.dataParser.incorrectSchemeType", sheetInfo.schemeType.toLowerCase, schemeName.toLowerCase),
         optionalParams = Seq(ersUtil.withArticle(sheetInfo.schemeType.toUpperCase), ersUtil.withArticle(schemeName.toUpperCase), sheetInfo.sheetName)))
     }
   }

  def getSheet(sheetName: String, scheme: String)(implicit messages: Messages): SheetInfo = {
    Logger.info(s"[DataGenerator][getSheet] Looking for sheetName: $sheetName")
    ersSheets.getOrElse(sheetName, {
      Logger.warn("[DataGenerator][getSheet] Couldn’t identify SheetName")
      val schemeName = ersUtil.getSchemeName(scheme)._2
      throw ERSFileProcessingException(
        "ers.exceptions.dataParser.incorrectSheetName",
        Messages("ers.exceptions.dataParser.unidentifiableSheetName") + " " + sheetName,
        needsExtendedInstructions = true,
        optionalParams = Seq(sheetName, schemeName)
      )
    })
  }

  def getSheetCsv(sheetName: String, scheme: String)(implicit messages: Messages): Either[Throwable, (SheetInfo, String)] = { // IS CSV
    Logger.info(s"[DataGenerator][getSheet] Looking for sheetName: $sheetName")
    ersSheets.get(sheetName) match {
      case Some(sheetInfo) => Right((sheetInfo, ersUtil.getSchemeName(scheme)._2))
      case _ =>
        Logger.warn("[DataGenerator][getSheet] Couldn’t identify SheetName")
        val schemeName = ersUtil.getSchemeName(scheme)._2
        Left(ERSFileProcessingException(
          "ers.exceptions.dataParser.incorrectSheetName",
          Messages("ers.exceptions.dataParser.unidentifiableSheetName") + " " + sheetName,
          needsExtendedInstructions = true,
          optionalParams = Seq(sheetName, schemeName)))
    }
  }

  def validateHeaderRow(rowData:Seq[String], sheetName:String, scheme:String, fileName: String)(implicit messages: Messages): Int =
  {
    val headerFormat = "[^a-zA-Z0-9]"

    val header = getSheet(sheetName, scheme).headerRow.map(_.replaceAll(headerFormat,""))
    val data = rowData.take(header.size)
    val dataTrim = data.map(_.replaceAll(headerFormat,""))

    Logger.debug("5.3  case 9 sheetName =" + sheetName + "data = " + dataTrim + "header == -> " + header)
    if (dataTrim == header) {
      header.size
    } else {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      Logger.warn("Error while reading File + Incorrect ERS Template")
      throw ERSFileProcessingException(
        "ers.exceptions.dataParser.incorrectHeader",
        Messages("ers.exceptions.dataParser.headersDontMatch"),
        needsExtendedInstructions = true,
        optionalParams = Seq(sheetName, fileName)
      )
    }
  }

  def deliverDataIteratorMetrics(startTime:Long): Unit =
    metrics.dataIteratorTimer(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)

}
