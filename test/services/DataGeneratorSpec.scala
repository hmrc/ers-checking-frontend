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
import play.api.i18n.Messages
import services.headers.HeaderData
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.util.Try

/**
 * Created by raghu on 03/02/16.
 */
class DataGeneratorSpec extends PlaySpec with OneServerPerSuite with ScalaFutures with MockitoSugar with BeforeAndAfter with HeaderData{

  object dataGeneratorObj extends DataGenerator

  val testAct = List("","","","")
  "The File Processing Service" must {

    "Ensure there are no ampersands in the submitted xml" in {
      val expectedMessage = "Must not contain ampersands."
      val inputXml = "<table:table-row table:style-name='ro6'><table:table-cell office:date-value='2015-01-02' table:style-name='ce18' calcext:value-type='date'><text:p>2. Jan. 2015</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>y</text:p></table:table-cell><table:table-cell table:style-name='ce19' calcext:value-type='string'><text:p>12345678901</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>J0hn</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>A.</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Williams (nee Barratt)</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>A123456A</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12/B1234579789</text:p></table:table-cell><table:table-cell office:date-value='2015-01-03' table:style-name='ce21' calcext:value-type='date'><text:p>2015-01-03</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Company & <text:s></text:s>Sons</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12 Example &Street</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>2nd Star On The Right and <text:s></text:s>Straight On Til Morning</text:p></table:table-cell><table:table-cell table:style-name='Default' calcext:value-type='string'><text:p>Exampley</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Example-upon-River</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>United Kingdom</text:p></table:table-cell><table:table-cell table:style-name='Default' calcext:value-type='string'><text:p>EX12 3AM</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB123549123</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='123456790012'><text:p>123456790012</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>123/XZ12345678</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Another Company Name</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12 Test Street</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Test</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Testing</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Testing-upon-River</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>United Kingdom</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>TE12 3ST</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB123456</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='1234567890'><text:p>1234567890</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>123/DC12345678</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='123.12'><text:p>123.12</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234' table:number-columns-repeated='2'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB12345678</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string' table:number-columns-repeated='3'><text:p>yes</text:p></table:table-cell><table:table-cell table:number-columns-repeated='982'></table:table-cell></table:table-row>"

      val thrown = intercept[ERSFileProcessingException] {dataGeneratorObj.validateSpecialCharacters(inputXml)}

      thrown.getMessage must be (expectedMessage)
     }

    "validateHeaderRow " in {
      dataGeneratorObj.validateHeaderRow(XMLTestData.otherHeaderSheet1Data, "Other_Grants_V3", "OTHER", "Other_Grants_V3.csv") must be (4)
      val result = Try(dataGeneratorObj.validateHeaderRow(XMLTestData.otherHeaderSheet1Data, "csopHeaderSheet1Data", "CSOP", "CSOP_OptionsGranted_V3.csv"))
      result.isFailure must be (true)
    }

    "validate CSOP_OptionsGranted_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(csopHeaderSheet1Data, "CSOP_OptionsGranted_V3", "CSOP", "CSOP_OptionsGranted_V3.csv") must be (9)
    }
    "validate CSOP_OptionsRCL_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(csopHeaderSheet2Data, "CSOP_OptionsRCL_V3", "CSOP", "CSOP_OptionsRCL_V3.csv") must be (9)
    }
    "validate CSOP_OptionsExercised_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(csopHeaderSheet3Data, "CSOP_OptionsExercised_V3", "CSOP", "CSOP_OptionsExercised_V3.csv") must be (20)
    }

    "validate SIP_Awards_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(sipHeaderSheet1Data, "SIP_Awards_V3", "SIP", "SIP_Awards_V3.csv") must be (17)
    }
    "validate SIP_Out_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(sipHeaderSheet2Data, "SIP_Out_V3", "SIP", "SIP_Out_V3.csv") must be (17)
    }

    "validate EMI40_Adjustments_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(emiHeaderSheet1Data, "EMI40_Adjustments_V3", "EMI", "EMI40_Adjustments_V3.csv") must be (14)
    }
    "validate EMI40_Replaced_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(emiHeaderSheet2Data, "EMI40_Replaced_V3", "EMI", "EMI40_Replaced_V3.csv") must be (17)
    }
    "validate EMI40_RLC_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(emiHeaderSheet3Data, "EMI40_RLC_V3", "EMI", "EMI40_RLC_V3.csv") must be (12)
    }
    "validate EMI40_NonTaxable_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(emiHeaderSheet4Data, "EMI40_NonTaxable_V3", "EMI", "EMI40_NonTaxable_V3.csv") must be (15)
    }
    "validate EMI40_Taxable_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(emiHeaderSheet5Data, "EMI40_Taxable_V3", "EMI", "EMI40_Taxable_V3.csv") must be (20)
    }

    "validate Other_Grants_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet1Data, "Other_Grants_V3", "OTHER", "Other_Grants_V3.csv") must be (4)
    }
    "validate Other_Options_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet2Data, "Other_Options_V3", "OTHER", "Other_Options_V3.csv") must be (42)
    }
    "validate Other_Acquisition_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet3Data, "Other_Acquisition_V3", "OTHER", "Other_Acquisition_V3.csv") must be (40)
    }
    "validate Other_RestrictedSecurities_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet4Data, "Other_RestrictedSecurities_V3", "OTHER", "Other_RestrictedSecurities_V3.csv") must be (20)
    }
    "validate Other_OtherBenefits_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet5Data, "Other_OtherBenefits_V3", "OTHER", "Other_OtherBenefits_V3.csv") must be (13)
    }
    "validate Other_Convertible_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet6Data, "Other_Convertible_V3", "OTHER", "Other_Convertible_V3.csv") must be (15)
    }
    "validate Other_Notional_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet7Data, "Other_Notional_V3", "OTHER", "Other_Notional_V3.csv") must be (13)
    }
    "validate Other_Enhancement_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet8Data, "Other_Enhancement_V3", "OTHER", "Other_Enhancement_V3.csv") must be (14)
    }
    "validate Other_Sold_V3 headerRow as valid" in {
      dataGeneratorObj.validateHeaderRow(otherHeaderSheet9Data, "Other_Sold_V3", "OTHER", "Other_Sold_V3.csv") must be (14)
    }

    "identifyAndDefineSheet with correct scheme type" in  {
      val hc = HeaderCarrier()
      dataGeneratorObj.identifyAndDefineSheet("EMI40_Adjustments_V3","2")(hc,Fixtures.buildFakeRequestWithSessionId("GET")) must be ("EMI40_Adjustments_V3")
      val result = Try(dataGeneratorObj.identifyAndDefineSheet("EMI40_Adjustments","2")(hc,Fixtures.buildFakeRequestWithSessionId("GET")))
      result.isFailure must be (true)
    }

    "raise exception if sheetName cannot be identified" in {
      val hc = HeaderCarrier()
      val invalidSheet = intercept[ERSFileProcessingException]{
        dataGeneratorObj.identifyAndDefineSheet("CSOP_OptionsExercised_V3","2")(hc,Fixtures.buildFakeRequestWithSessionId("GET"))
      }
      invalidSheet.message mustBe Messages("ers.exceptions.dataParser.incorrectSchemeType","a CSOP", "an EMI", "CSOP_OptionsExercised_V3")
    }

    "isBlankRow" in {
      dataGeneratorObj.isBlankRow(testAct) must be (true)
      val testAct1 = List("dfgdg","","","")
      dataGeneratorObj.isBlankRow(testAct1) must be (false)
    }

    "get an exception if ods file has less than 9 rows and doesn't have header data" in {
      object dataGenObj extends DataGenerator
      val result = intercept[ERSFileProcessingException] {
        dataGenObj.getErrors(XMLTestData.getInvalidCSOPWithoutHeaders,"1","CSOP.ods")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      }
      result.message mustBe Messages("ers.exceptions.dataParser.incorrectHeader", "CSOP_OptionsGranted_V3", "CSOP.ods")
    }

    "get an exception if ods file has more than 1 sheet but 1 of the sheets has less than 9 rows and doesn't have header data" in {
      object dataGenObj extends DataGenerator
      val result = intercept[ERSFileProcessingException] {
        dataGenObj.getErrors(XMLTestData.getInvalidCSOPWith2Sheets1WithoutHeaders,"1","CSOP.ods")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      }
      result.message mustBe Messages("ers.exceptions.dataParser.incorrectHeader", "CSOP_OptionsGranted_V3", "CSOP.ods")
    }

    "get an exception if ods file doesn't contain any data" in {
      object dataGenObj extends DataGenerator
      val result = intercept[ERSFileProcessingException] {
        dataGenObj.getErrors(XMLTestData.getCSOPWithoutData,"1","CSOP.ods")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      }
      result.message mustBe Messages("ers.exceptions.dataParser.noData")
    }

    "get no errors for EMI" in {
      object dataGenObj extends DataGenerator
      val result = dataGenObj.getErrors(XMLTestData.getEMIAdjustmentsTemplate,"2","")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      result.foreach(_.errors.size mustBe 0)
    }

    "collect errors in the first sheet of EMI" in {
      object dataGenObj extends DataGenerator
      val result = dataGenObj.getErrors(XMLTestData.getInvalidEMIAdjustmentsTemplate,"2","")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      result(0).errors.size mustBe 1
    }

    "collect errors in second sheet of EMI" in {
      object dataGenObj extends DataGenerator
      val result = dataGenObj.getErrors(XMLTestData.getEMIAdjustmentsTemplate ++ XMLTestData.getInvalidEMIReplacedTemplate,"2","")(Fixtures.buildFakeUser,hc = HeaderCarrier(),Fixtures.buildFakeRequestWithSessionId("GET"))
      result(1).errors.size mustBe 1
    }

  }

}
