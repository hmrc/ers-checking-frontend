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

import javax.xml.parsers.SAXParserFactory
import models._
import play.api.Logger
import play.api.i18n.Messages

import scala.util.Try
import scala.xml._

trait DataParser {

  val repeatColumnsAttr = "table:number-columns-repeated"
  val repeatTableAttr = "table:number-rows-repeated"

  def validateSpecialCharacters(xmlRowData : String )(implicit messages: Messages): Unit ={
    if(xmlRowData.contains("&")){
      Logger.debug("[DataParser][validateSpecialCharacters] Found invalid xml in Data Parser, throwing exception")
      throw ERSFileProcessingException(Messages("ers.exceptions.dataParser.ampersand"), Messages("ers.exceptions.dataParser.parsingOfFileData"))
    }
  }

  val secureSAXParser: SAXParser = {
    val saxParserFactory = SAXParserFactory.newInstance()
    saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    saxParserFactory.newSAXParser()
  }

  def parse(row:String, fileName : String)(implicit messages: Messages): Either[String, (Seq[String], Int)] = {
    Logger.debug("DataParser: Parse: About to parse row: " + row)

    val xmlRow = Try(Option(XML.withSAXParser(secureSAXParser)loadString(row))).getOrElse(None)

    xmlRow match {
      case None =>
        Logger.debug("[DataParser][parse] 3.1 Parse row left ")
        validateSpecialCharacters(row)
        Left(row)
      case _:Option[Elem] => Logger.debug("[DataParser][parse] 3.2 Parse row right ")
        val cols = Try( Right(xmlRow.get.child.flatMap(parseColumn))).getOrElse{
          Logger.warn(Messages("ers.exceptions.dataParser.fileRetrievalFailed", fileName))
          throw ERSFileProcessingException (
            Messages("ers.exceptions.dataParser.fileRetrievalFailed", fileName),
            Messages("ers.exceptions.dataParser.parserFailure", fileName)
          )
        }

        cols match {
          case Right(r: Seq[String]) if !isBlankRow(r) => Right(r, repeated(xmlRow))
          case Right(s: Seq[String]) => Right((s, 1))
        }
      case _  =>
        Logger.warn(Messages("ers.exceptions.dataParser.fileParsingError", fileName))
        throw ERSFileProcessingException(
          Messages("ers.exceptions.dataParser.fileParsingError", fileName),
          Messages("ers.exceptions.dataParser.parsingOfFileData")
        )
    }
  }

  def repeated(xmlRow: Option[Elem]): Int = {
    val rowsRepeated = xmlRow.get.attributes.asAttrMap.get(repeatTableAttr)
    if (rowsRepeated.isDefined) {
      rowsRepeated.get.toInt
    } else {
      1
    }
  }

  def parseColumn(col:scala.xml.Node): Seq[String] = {
    val colsRepeated =  col.attributes.asAttrMap.get(repeatColumnsAttr)

    if(colsRepeated.nonEmpty && colsRepeated.get.toInt < 50) {
      val cols:scala.collection.mutable.MutableList[String]= scala.collection.mutable.MutableList()
      for( _ <- 1 to colsRepeated.get.toInt)  cols += col.text
      cols.toSeq
    }
    else Seq(col.text)
  }

  def isBlankRow(data :Seq[String]): Boolean = data.mkString("").trim.length == 0
}