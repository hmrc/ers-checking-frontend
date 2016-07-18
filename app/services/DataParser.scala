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

import java.util.concurrent.TimeUnit

import hmrc.gsi.gov.uk.services.validation._
import metrics.Metrics
import models._
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Request
import services.ERSTemplatesInfo._
import services.audit.AuditEvents
import services.validation.ErsValidator
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.ContentUtil

import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.xml._

trait DataParser {

  val repeatAttr = "table:number-columns-repeated"
  def validateSpecialCharacters(xmlRowData : String )={
    if(xmlRowData.contains("&")){
      Logger.debug("Found invalid xml in Data Parser, throwing exception")
      throw new ERSFileProcessingException("Invalid characters found in file: Ampersand (&) characters are not allowed in submissions.", "Found error in the data parser")
    }
  }

  def parse(row:String, fileName : String) = {
    Logger.debug("DataParser: Parse: About to parse row: " + row)

    val xmlRow = Try(Option(XML.loadString(row))).getOrElse(None)
    //    Logger.debug("DataParser: Parse: About to match xmlRow: " + xmlRow)

    xmlRow match {
      case None => {
        Logger.debug("3.1 Parse row left ")
        validateSpecialCharacters(row)
        Left(row)
      }
      case elem:Option[Elem] => Logger.debug("3.2 Parse row right ")
        Try( Right(xmlRow.get.child.flatMap( parseColumn(_)))).getOrElse{
          Logger.warn(Messages("ers.exceptions.dataParser.fileRetrievalFailed", row))
          throw ERSFileProcessingException (Messages("ers.exceptions.dataParser.fileRetrievalFailed", row), Messages("ers.exceptions.dataParser.parserFailure"))
        }
      case _  => {
        Logger.warn(Messages("ers.exceptions.dataParser.fileParsingError", fileName))
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.fileParsingError", fileName), Messages("ers.exceptions.dataParser.parsingOfFileData"))
      }
    }
  }

  def parseColumn(col:scala.xml.Node) = {
    val colsRepeated =  col.attributes.asAttrMap.get(repeatAttr)

    if(colsRepeated.nonEmpty && colsRepeated.get.toInt < 50) {
      val cols:scala.collection.mutable.MutableList[String]= scala.collection.mutable.MutableList()
      for( i <- 1 to colsRepeated.get.toInt)  cols += col.text
      cols.toSeq
    }
    else  Seq(col.text)
  }

}

object DataGenerator extends DataGenerator

trait DataGenerator extends DataParser with Metrics{

  def getErrors(iterator:Iterator[String], scheme:String, fileName : String)(implicit authContext: AuthContext, hc: HeaderCarrier, request: Request[_]) =
  {
    var rowNum = 0
    implicit var sheetName :String = ""
    var sheetColSize = 0
    val schemeErrors: ListBuffer[SheetErrors] = ListBuffer()
    var validator:DataValidator = ERSValidationConfigs.defValidator
    def incRowNum() = rowNum =  rowNum + 1

    var rowCount : Int = 0
    var rowsWithData : Int = 0

    val startTime = System.currentTimeMillis()

    while(iterator.hasNext){

      val row = iterator.next()
      //Logger.debug(" Data before  parsing ---> " + row)
      val rowData = parse(row, fileName)
      Logger.debug(" parsed data ---> " + rowData + " -- cursor --> " + rowNum)
      rowData.isLeft match {
        case true => {
          Logger.debug("data from the left --->" + rowData.left.get)
          sheetName = identifyAndDefineSheet(rowData.left.get,scheme)
          Logger.debug("Sheetname = " + sheetName + "******")
          schemeErrors += SheetErrors(sheetName, ListBuffer())
          Logger.debug("SchemeData = " + schemeErrors.size + "******")
          rowNum = 1
          validator = setValidator(sheetName)
        }
        case _ => rowNum match {
          case count if count < 9 => {
            Logger.debug("GetData: incRowNum if count < 9: " + count + " RowNum: " + rowNum )
            incRowNum()
          }
          case 9 => {
            Logger.debug("GetData: incRowNum if  9: " + rowNum + "sheetColSize: " + sheetColSize )
            Logger.debug("sheetName--->" + sheetName)
            sheetColSize = validateHeaderRow(rowData.right.get, sheetName)
            incRowNum()
          }
          case _ => {
            val foundData = rowData.right.get
            rowCount = rowData.right.get.size

            val data = if(foundData.size < sheetColSize) {
              Logger.warn(s"Difference between amount of columns ${foundData.size} and amount of headers ${sheetColSize}")
              val additionalEmptyCells: Seq[String] = List.fill(sheetColSize - foundData.size)("")
              (foundData ++ additionalEmptyCells).take(sheetColSize)
            }
            else {
              foundData.take(sheetColSize)
            }

            if(!isBlankRow(data)){
              rowsWithData+=1
              ErsValidator.validateRow(data,rowNum,validator) match {
                case errors:Option[List[ValidationError]] if errors.isDefined => {
                  Logger.debug("Error while Validating File + Formatting errors present " + errors.toString)
                  implicit val hc:HeaderCarrier = new HeaderCarrier()
                  //                  errors.map{
                  //                    AuditEvents.validationErrorAudit(_,SchemeData(schemeInfo,sheetName,new List[ValidationError]))
                  //                  }
                    schemeErrors.last.errors ++= errors.get

                }
                case _ => schemeErrors.last.errors
              }
            }
            incRowNum()
          }
        }
      }
    }
    deliverDataIteratorMetrics(startTime)
    AuditEvents.numRowsInSchemeData(scheme, rowsWithData)(authContext,hc,request)
    Logger.debug("The SchemeData that GetData finally returns: " + schemeErrors)
    schemeErrors
  }

  def setValidator(sheetName:String)(implicit hc : HeaderCarrier, request: Request[_]) = {
    try {
      ERSValidationConfigs.getValidator(ersSheets(sheetName).configFileName)
    }catch{
      case e:Exception => {
        AuditEvents.auditRunTimeError(e,"Could not set the validator",sheetName)(hc,request)
        Logger.error("setValidator has thrown an exception, SheetName: " + sheetName + " Exception message: " + e.getMessage)
        throw new ERSFileProcessingException(Messages("ers.exceptions.dataParser.configFailure", sheetName), "Could not set the validator ")
      }
    }
  }

  def identifyAndDefineSheet(data:String,scheme:String)(implicit headerCarrier: HeaderCarrier, request: Request[_]) = {
    Logger.debug("5.1  case 0 identifyAndDefineSheet  " )
    val res = getSheet(data)
    val schemeName = ContentUtil.getSchemeName(scheme)._2
    res.schemeType.toLowerCase == schemeName.toLowerCase match {
      case true =>  {
        Logger.debug("****5.1.1  data contains data:  *****" + data)
        data }
      case _ => {
        AuditEvents.fileProcessingErrorAudit(res.schemeType, res.sheetName, s"${res.schemeType.toLowerCase} is not equal to ${schemeName.toLowerCase}")
        Logger.warn(Messages("ers.exceptions.dataParser.incorrectSchemeType", res.schemeType.toLowerCase, schemeName.toLowerCase))
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectSchemeType", res.schemeType.toLowerCase, schemeName.toLowerCase), Messages("ers.exceptions.dataParser.incorrectSchemeType", res.schemeType.toLowerCase, schemeName.toLowerCase))
      }
    }
  }

  def getSheet(sheetName:String) = {
    Logger.info(s"Looking for sheetName: ${sheetName}")
    ersSheets.getOrElse(sheetName, {
     //  implicit val hc:HeaderCarrier = new HeaderCarrier()
    //  AuditEvents.fileProcessingErrorAudit(schemeInfo, sheetName, "Could not set the validator")
      Logger.warn(Messages("ers.exceptions.dataParser.unidentifiableSheetName") + sheetName)
      throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectSheetName", sheetName), Messages("ers.exceptions.dataParser.unidentifiableSheetName") + " " +sheetName)
    })
  }

  def validateHeaderRow(rowData:Seq[String], sheetName:String) =
  {
    val headerFormat = "[^a-zA-Z0-9]"

    val header = getSheet(sheetName).headerRow.map(_.replaceAll(headerFormat,""))
    val data = rowData.take(header.size)
    val dataTrim = data.map(_.replaceAll(headerFormat,""))

    Logger.debug("5.3  case 9 sheetName =" + sheetName + "data = " + dataTrim + "header == -> " + header)
    dataTrim == header  match {
      case true=> header.size
      case _ => {
        implicit val hc:HeaderCarrier = new HeaderCarrier()
     //   AuditEvents.fileProcessingErrorAudit(schemeInfo,sheetName,"Header row invalid")
        Logger.warn("Error while reading File + Incorrect ERS Template")
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectHeader", sheetName),Messages("ers.exceptions.dataParser.headersDontMatch"))
      }
    }
  }

  def isBlankRow(data :Seq[String]) = data.mkString("").trim.length == 0

  def deliverDataIteratorMetrics(startTime:Long) =
    metrics.dataIteratorTimer(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)

}
