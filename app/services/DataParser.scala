/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.xml.parsers.SAXParserFactory
import metrics.Metrics
import models._
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Request
import services.ERSTemplatesInfo._
import services.audit.AuditEvents
import services.validation.ErsValidator
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.services.validation.DataValidator
import utils.{ContentUtil, ParserUtil}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.xml._
import uk.gov.hmrc.http.HeaderCarrier

trait DataParser {

  val repeatColumnsAttr = "table:number-columns-repeated"
  val repeatTableAttr = "table:number-rows-repeated"

  def validateSpecialCharacters(xmlRowData : String )={
    if(xmlRowData.contains("&")){
      Logger.debug("Found invalid xml in Data Parser, throwing exception")
      throw new ERSFileProcessingException(Messages("ers.exceptions.dataParser.ampersand"), Messages("ers.exceptions.dataParser.parsingOfFileData"))
    }
  }


  def secureSAXParser = {
    val saxParserFactory = SAXParserFactory.newInstance()
    saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    saxParserFactory.newSAXParser()
  }

  def parse(row:String, fileName : String): Either[String, (Seq[String], Int)] = {
    Logger.debug("DataParser: Parse: About to parse row: " + row)

    val xmlRow = Try(Option(XML.withSAXParser(secureSAXParser)loadString(row))).getOrElse(None)
    //    Logger.debug("DataParser: Parse: About to match xmlRow: " + xmlRow)

    xmlRow match {
      case None => {
        Logger.debug("3.1 Parse row left ")
        validateSpecialCharacters(row)
        Left(row)
      }
      case elem:Option[Elem] => Logger.debug("3.2 Parse row right ")
        val cols = Try( Right(xmlRow.get.child.flatMap( parseColumn(_)))).getOrElse{
          Logger.warn(Messages("ers.exceptions.dataParser.fileRetrievalFailed", fileName))
          throw ERSFileProcessingException (Messages("ers.exceptions.dataParser.fileRetrievalFailed", fileName), Messages("ers.exceptions.dataParser.parserFailure", fileName))
        }

        cols match {
          case Right(r: Seq[String]) if !isBlankRow(r) => Right(r, repeated(xmlRow))
          case Right(s: Seq[String]) => Right((s, 1))
        }
      case _  => {
        Logger.warn(Messages("ers.exceptions.dataParser.fileParsingError", fileName))
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.fileParsingError", fileName), Messages("ers.exceptions.dataParser.parsingOfFileData"))
      }
    }
  }

  def repeated(xmlRow: Option[Elem]): Int = {
    val rowsRepeated = xmlRow.get.attributes.asAttrMap.get(repeatTableAttr)
    if (rowsRepeated.isDefined) {
      rowsRepeated.get.toInt
    }
    else {
      1
    }
  }

  def parseColumn(col:scala.xml.Node): Seq[String] = {
    val colsRepeated =  col.attributes.asAttrMap.get(repeatColumnsAttr)

    if(colsRepeated.nonEmpty && colsRepeated.get.toInt < 50) {
      val cols:scala.collection.mutable.MutableList[String]= scala.collection.mutable.MutableList()
      for( i <- 1 to colsRepeated.get.toInt)  cols += col.text
      cols.toSeq
    }
    else  Seq(col.text)
  }

  def isBlankRow(data :Seq[String]) = data.mkString("").trim.length == 0

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

    def checkForMissingHeaders(rowNum: Int, sheetName: String) = {
      if(rowNum > 0 && rowNum < 9) {
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectHeader", sheetName, fileName),Messages("ers.exceptions.dataParser.incorrectHeader", sheetName, fileName), needsExtendedInstructions = true)
      }
    }

    while(iterator.hasNext){
      val row = iterator.next()
      //Logger.debug(" Data before  parsing ---> " + row)
      val rowData = parse(row, fileName)
      Logger.debug(" parsed data ---> " + rowData + " -- cursor --> " + rowNum)
      rowData.isLeft match {
        case true => {
          checkForMissingHeaders(rowNum, sheetName)
          Logger.debug("data from the left --->" + rowData.left.get)
          sheetName = identifyAndDefineSheet(rowData.left.get,scheme)
          Logger.debug("Sheetname = " + sheetName + "******")
          schemeErrors += SheetErrors(sheetName, ListBuffer())
          Logger.debug("SchemeData = " + schemeErrors.size + "******")
          rowNum = 1
          validator = setValidator(sheetName)
        }
        case _ =>
          for (i <- 1 to rowData.right.get._2) {
            rowNum match {
              case count if count < 9 => {
                Logger.debug("GetData: incRowNum if count < 9: " + count + " RowNum: " + rowNum)
                incRowNum()
              }
              case 9 => {
                Logger.debug("GetData: incRowNum if  9: " + rowNum + "sheetColSize: " + sheetColSize)
                Logger.debug("sheetName--->" + sheetName)
                sheetColSize = validateHeaderRow(rowData.right.get._1, sheetName, scheme, fileName)
                incRowNum()
              }
              case _ => {
                val foundData = rowData.right.get._1
                rowCount = rowData.right.get._1.size

                val data = ParserUtil.formatDataToValidate(foundData, sheetName)
                if (!isBlankRow(data)) {
                  rowsWithData += 1
                  ErsValidator.validateRow(validator)(data, rowNum) match {
                    case Some(errors) if errors.nonEmpty => {
                      Logger.debug("Error while Validating File + Formatting errors present " + errors.toString)
                      schemeErrors.last.errors ++= errors
                    }
                    case _ => schemeErrors.last.errors
                  }
                }
                incRowNum()
              }
            }
          }
      }
    }

    checkForMissingHeaders(rowNum, sheetName)
    if(rowsWithData == 0) {
      throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.noData"),Messages("ers.exceptions.dataParser.noData"), needsExtendedInstructions = true)
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
    val res = getSheet(data, scheme)
    val schemeName = ContentUtil.getSchemeName(scheme)._2
    res.schemeType.toLowerCase == schemeName.toLowerCase match {
      case true =>  {
        Logger.debug("****5.1.1  data contains data:  *****" + data)
        data }
      case _ => {
        AuditEvents.fileProcessingErrorAudit(res.schemeType, res.sheetName, s"${res.schemeType.toLowerCase} is not equal to ${schemeName.toLowerCase}")
        Logger.warn(Messages("ers.exceptions.dataParser.incorrectSchemeType", res.schemeType.toUpperCase, schemeName.toUpperCase))
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectSchemeType", ContentUtil.withArticle(res.schemeType.toUpperCase), ContentUtil.withArticle(schemeName.toUpperCase), res.sheetName), Messages("ers.exceptions.dataParser.incorrectSchemeType", res.schemeType.toLowerCase, schemeName.toLowerCase))
      }
    }
  }

  def getSheet(sheetName:String, scheme:String) = {
    Logger.info(s"Looking for sheetName: ${sheetName}")
    ersSheets.getOrElse(sheetName, {
     //  implicit val hc:HeaderCarrier = new HeaderCarrier()
    //  AuditEvents.fileProcessingErrorAudit(schemeInfo, sheetName, "Could not set the validator")
      Logger.warn(Messages("ers.exceptions.dataParser.unidentifiableSheetName") + sheetName)
      val schemeName = ContentUtil.getSchemeName(scheme)._2
      throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectSheetName", sheetName, schemeName), Messages("ers.exceptions.dataParser.unidentifiableSheetName") + " " +sheetName, needsExtendedInstructions = true)
    })
  }

  def validateHeaderRow(rowData:Seq[String], sheetName:String, scheme:String, fileName: String) =
  {
    val headerFormat = "[^a-zA-Z0-9]"

    val header = getSheet(sheetName, scheme).headerRow.map(_.replaceAll(headerFormat,""))
    val data = rowData.take(header.size)
    val dataTrim = data.map(_.replaceAll(headerFormat,""))

    Logger.debug("5.3  case 9 sheetName =" + sheetName + "data = " + dataTrim + "header == -> " + header)
    dataTrim == header  match {
      case true=> header.size
      case _ => {
        implicit val hc:HeaderCarrier = new HeaderCarrier()
     //   AuditEvents.fileProcessingErrorAudit(schemeInfo,sheetName,"Header row invalid")
        Logger.warn("Error while reading File + Incorrect ERS Template")
        throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.incorrectHeader", sheetName, fileName),Messages("ers.exceptions.dataParser.headersDontMatch"), needsExtendedInstructions = true)
      }
    }
  }

  def deliverDataIteratorMetrics(startTime:Long) =
    metrics.dataIteratorTimer(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)

}
