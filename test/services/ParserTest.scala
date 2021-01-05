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

import controllers.Fixtures
import helpers.ErsTestHelper
import models.ERSFileProcessingException
import org.mockito.ArgumentMatchers
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.DefaultMessagesControllerComponents
import services.XMLTestData._
import utils.ParserUtil
import org.mockito.Mockito._


class ParserTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures with ErsTestHelper with BeforeAndAfter {

  val mockParserUtil: ParserUtil = mock[ParserUtil]
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  object TestDataParser extends DataParser
  object TestDataGenerator extends DataGenerator(mockAuditEvents, mockMetrics, mockParserUtil, mockErsUtil)

  when(mockErsUtil.withArticle(ArgumentMatchers.any())).thenReturn("article")

  "parse row with duplicate column data 1" in {
    val result = TestDataParser.parse(emiAdjustmentsXMLRow1.toString,"")
    result.right.get._1.size must equal(17)
  }

  besParserTests.foreach( rec => {
    rec._1 in {
      val result = TestDataParser.parse(rec._2.toString,"")
      result.right.get._1.toList.take(rec._3.size) must be (rec._3)
    }
  })


  "display incorrectSheetName exception in identifyAndDefineSheet method" in {

    when(mockErsUtil.getSchemeName(ArgumentMatchers.any())).thenReturn(("ers_pdf_error_report.emi", "EMI"))

    val thrown = the[ERSFileProcessingException] thrownBy
      TestDataGenerator.identifyAndDefineSheet("EMI40_Taxable","EMI")(hc,Fixtures.buildFakeRequestWithSessionId("GET"), implicitly[Messages])

    thrown.getMessage mustBe "ers.exceptions.dataParser.incorrectSheetName"
    thrown.optionalParams mustBe Seq("EMI40_Taxable", "EMI")
  }

  "display incorrectHeader exception in validateHeaderRow method" in {

    val thrown = the[ERSFileProcessingException] thrownBy
      TestDataGenerator.validateHeaderRow(Seq("",""), "CSOP_OptionsRCL_V3", "CSOP", "CSOP_OptionsRCL_V3.csv")

    thrown.getMessage mustBe "ers.exceptions.dataParser.incorrectHeader"
    thrown.optionalParams mustBe Seq("CSOP_OptionsRCL_V3", "CSOP_OptionsRCL_V3.csv")
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
    when(mockErsUtil.getSchemeName(ArgumentMatchers.any())).thenReturn(("ers_pdf_error_report.csop", "CSOP"))

    val result = intercept[ERSFileProcessingException]{
      TestDataGenerator.getSheet("abc", "csop")
    }
    result.message mustBe "ers.exceptions.dataParser.incorrectSheetName"
    result.optionalParams mustBe Seq("abc", "CSOP")
  }

}
