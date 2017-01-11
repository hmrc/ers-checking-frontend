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

import controllers.Fixtures
import models.ERSFileProcessingException
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import services.XMLTestData._
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.i18n.Messages.Implicits._

class ParserTest extends PlaySpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter{

  implicit val messages = applicationMessages
  object TestDataParser extends DataParser
  object TestDataGenerator extends DataGenerator

  "parse row with duplicate column data 1" in {
    val result = TestDataParser.parse(emiAdjustmentsXMLRow1.toString,"")
    result.right.get.size must equal(17)
  }

  besParserTests.foreach( rec => {
    rec._1 in {
      val result = TestDataParser.parse(rec._2.toString,"")
      result.right.get.toList.take(rec._3.size) must be (rec._3)
    }
  })


  "display incorrectSheetName exception in identifyAndDefineSheet method" in {
    def exceptionMessage: String = {
      try {
        val hc = HeaderCarrier()
        val result = TestDataGenerator.identifyAndDefineSheet("EMI40_Taxable","2")(hc,Fixtures.buildFakeRequestWithSessionId("GET"))
        result.toString()
      }
      catch {
        case e: ERSFileProcessingException => {
          return e.message
        }
      }
    }
    exceptionMessage mustBe messages("ers.exceptions.dataParser.incorrectSheetName", "EMI40_Taxable", "EMI")
  }

  "display incorrectHeader exception in validateHeaderRow method" in {
    def exceptionMessage: String = {
      try {
        val data: Seq[String] = Seq("","")
        val result = TestDataGenerator.validateHeaderRow(data, "CSOP_OptionsRCL_V3", "CSOP", "CSOP_OptionsRCL_V3.csv")
        result.toString()
      }
      catch {
        case e: ERSFileProcessingException => {
          return e.message
        }
      }
    }
    exceptionMessage mustBe messages("ers.exceptions.dataParser.incorrectHeader", "CSOP_OptionsRCL_V3", "CSOP_OptionsRCL_V3.csv")
  }

  "return sheetInfo given a valid sheet name" in {
    val sheet = TestDataGenerator.getSheet(ERSTemplatesInfo.emiSheet5Name, "EMI")
    sheet.schemeType mustBe "EMI"
    sheet.sheetId mustBe 5
  }

  "return sheetInfo for CSOP_OptionsGranted_V3" in {
    val sheet = TestDataGenerator.getSheet(ERSTemplatesInfo.csopSheet1Name, "CSOP")
    sheet.schemeType mustBe "CSOP"
    sheet.sheetId mustBe 1
  }

  "return sheetInfo for CSOP_OptionsRCL_V3" in {
    val sheet = TestDataGenerator.getSheet(ERSTemplatesInfo.csopSheet2Name, "CSOP")
    sheet.schemeType mustBe "CSOP"
    sheet.sheetId mustBe 2
  }

  "return sheetInfo for CSOP_OptionsExercised_V3" in {
    val sheet = TestDataGenerator.getSheet(ERSTemplatesInfo.csopSheet3Name, "CSOP")
    sheet.schemeType mustBe "CSOP"
    sheet.sheetId mustBe 3
  }

  "throw an exception for an invalid sheetName" in {
    val result = intercept[ERSFileProcessingException]{
      TestDataGenerator.getSheet("abc", "1")
    }
    result.message mustBe messages("ers.exceptions.dataParser.incorrectSheetName", "abc", "CSOP")
  }

}
