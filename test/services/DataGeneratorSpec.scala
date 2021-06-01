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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.DefaultMessagesControllerComponents
import services.ERSTemplatesInfo._
import services.headers.HeaderData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.services.validation.DataValidator
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.ParserUtil

import scala.util.Try

class DataGeneratorSpec extends PlaySpec with GuiceOneServerPerSuite with ErsTestHelper with HeaderData {

  lazy val mockParserUtil: ParserUtil = mock[ParserUtil]
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  lazy val testParserUtil: ParserUtil = fakeApplication.injector.instanceOf[ParserUtil]
  lazy val testErsValidationConfigs: ERSValidationConfigs = fakeApplication.injector.instanceOf[ERSValidationConfigs]

  class DataGeneratorObj(scheme: String) extends DataGenerator(mockAuditEvents, mockMetrics,
    testParserUtil, testErsValidationConfigs, mockErsUtil, mockErsValidator){
    when(mockErsUtil.getSchemeName(any())).thenReturn((s"ers_pdf_error_report.${scheme.toLowerCase}", scheme))
  }

  "The File Processing Service" must {

    "Ensure there are no ampersands in the submitted xml" in new DataGeneratorObj("CSOP") {
      val expectedMessage = "Must not contain ampersands."
      val inputXml = "<table:table-row table:style-name='ro6'><table:table-cell office:date-value='2015-01-02' table:style-name='ce18' calcext:value-type='date'><text:p>2. Jan. 2015</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>y</text:p></table:table-cell><table:table-cell table:style-name='ce19' calcext:value-type='string'><text:p>12345678901</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>J0hn</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>A.</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Williams (nee Barratt)</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>A123456A</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12/B1234579789</text:p></table:table-cell><table:table-cell office:date-value='2015-01-03' table:style-name='ce21' calcext:value-type='date'><text:p>2015-01-03</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Company & <text:s></text:s>Sons</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12 Example &Street</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>2nd Star On The Right and <text:s></text:s>Straight On Til Morning</text:p></table:table-cell><table:table-cell table:style-name='Default' calcext:value-type='string'><text:p>Exampley</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Example-upon-River</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>United Kingdom</text:p></table:table-cell><table:table-cell table:style-name='Default' calcext:value-type='string'><text:p>EX12 3AM</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB123549123</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='123456790012'><text:p>123456790012</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>123/XZ12345678</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Another Company Name</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>12 Test Street</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Test</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Testing</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>Testing-upon-River</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>United Kingdom</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>TE12 3ST</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB123456</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='1234567890'><text:p>1234567890</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>123/DC12345678</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='123.12'><text:p>123.12</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234' table:number-columns-repeated='2'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>AB12345678</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell calcext:value-type='string' table:number-columns-repeated='3'><text:p>yes</text:p></table:table-cell><table:table-cell table:number-columns-repeated='982'></table:table-cell></table:table-row>"

      val thrown: ERSFileProcessingException = intercept[ERSFileProcessingException] {validateSpecialCharacters(inputXml)}

      thrown.getMessage must be (expectedMessage)
     }

    "validateHeaderRow " in new DataGeneratorObj("OTHER") {
      validateHeaderRow(XMLTestData.otherHeaderSheet1Data, "Other_Grants_V3", "OTHER", "Other_Grants_V3.csv") must be (4)
      val result: Try[Int] = Try(validateHeaderRow(XMLTestData.otherHeaderSheet1Data, "csopHeaderSheet1Data", "CSOP", "CSOP_OptionsGranted_V3.csv"))
      result.isFailure must be (true)
    }

    "validate CSOP_OptionsGranted_V3 headerRow as valid" in new DataGeneratorObj("CSOP") {
      validateHeaderRow(csopHeaderSheet1Data, "CSOP_OptionsGranted_V3", "CSOP", "CSOP_OptionsGranted_V3.csv") must be (9)
    }
    "validate CSOP_OptionsRCL_V3 headerRow as valid" in new DataGeneratorObj("CSOP") {
      validateHeaderRow(csopHeaderSheet2Data, "CSOP_OptionsRCL_V3", "CSOP", "CSOP_OptionsRCL_V3.csv") must be (9)
    }
    "validate CSOP_OptionsExercised_V3 headerRow as valid" in new DataGeneratorObj("CSOP") {
      validateHeaderRow(csopHeaderSheet3Data, "CSOP_OptionsExercised_V3", "CSOP", "CSOP_OptionsExercised_V3.csv") must be (20)
    }

    "validate SIP_Awards_V3 headerRow as valid" in new DataGeneratorObj("SIP") {
      validateHeaderRow(sipHeaderSheet1Data, "SIP_Awards_V3", "SIP", "SIP_Awards_V3.csv") must be (17)
    }
    "validate SIP_Out_V3 headerRow as valid" in new DataGeneratorObj("SIP") {
      validateHeaderRow(sipHeaderSheet2Data, "SIP_Out_V3", "SIP", "SIP_Out_V3.csv") must be (17)
    }

    "validate EMI40_Adjustments_V3 headerRow as valid" in new DataGeneratorObj("EMI") {
      validateHeaderRow(emiHeaderSheet1Data, "EMI40_Adjustments_V3", "EMI", "EMI40_Adjustments_V3.csv") must be (14)
    }
    "validate EMI40_Replaced_V3 headerRow as valid" in new DataGeneratorObj("EMI") {
      validateHeaderRow(emiHeaderSheet2Data, "EMI40_Replaced_V3", "EMI", "EMI40_Replaced_V3.csv") must be (17)
    }
    "validate EMI40_RLC_V3 headerRow as valid" in new DataGeneratorObj("EMI") {
      validateHeaderRow(emiHeaderSheet3Data, "EMI40_RLC_V3", "EMI", "EMI40_RLC_V3.csv") must be (12)
    }
    "validate EMI40_NonTaxable_V3 headerRow as valid" in new DataGeneratorObj("EMI") {
      validateHeaderRow(emiHeaderSheet4Data, "EMI40_NonTaxable_V3", "EMI", "EMI40_NonTaxable_V3.csv") must be (15)
    }
    "validate EMI40_Taxable_V3 headerRow as valid" in new DataGeneratorObj("EMI") {
      validateHeaderRow(emiHeaderSheet5Data, "EMI40_Taxable_V3", "EMI", "EMI40_Taxable_V3.csv") must be (20)
    }

    "validate Other_Grants_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet1Data, "Other_Grants_V3", "OTHER", "Other_Grants_V3.csv") must be (4)
    }
    "validate Other_Options_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet2Data, "Other_Options_V3", "OTHER", "Other_Options_V3.csv") must be (42)
    }
    "validate Other_Acquisition_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet3Data, "Other_Acquisition_V3", "OTHER", "Other_Acquisition_V3.csv") must be (40)
    }
    "validate Other_RestrictedSecurities_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet4Data, "Other_RestrictedSecurities_V3", "OTHER", "Other_RestrictedSecurities_V3.csv") must be (20)
    }
    "validate Other_OtherBenefits_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet5Data, "Other_OtherBenefits_V3", "OTHER", "Other_OtherBenefits_V3.csv") must be (13)
    }
    "validate Other_Convertible_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet6Data, "Other_Convertible_V3", "OTHER", "Other_Convertible_V3.csv") must be (15)
    }
    "validate Other_Notional_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet7Data, "Other_Notional_V3", "OTHER", "Other_Notional_V3.csv") must be (13)
    }
    "validate Other_Enhancement_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet8Data, "Other_Enhancement_V3", "OTHER", "Other_Enhancement_V3.csv") must be (14)
    }
    "validate Other_Sold_V3 headerRow as valid" in new DataGeneratorObj("OTHER") {
      validateHeaderRow(otherHeaderSheet9Data, "Other_Sold_V3", "OTHER", "Other_Sold_V3.csv") must be (14)
    }

    "identifyAndDefineSheet with correct scheme type" in new DataGeneratorObj("EMI") {
      val hc: HeaderCarrier = HeaderCarrier()
      identifyAndDefineSheet("EMI40_Adjustments_V3","emi")(hc, implicitly[Messages]) must be ("EMI40_Adjustments_V3")
      val result: Try[String] = Try(identifyAndDefineSheet("EMI40_Adjustments","emi")(hc, implicitly[Messages]))
      result.isFailure must be (true)
    }

    "raise exception if sheetName cannot be identified" in new DataGeneratorObj("EMI") {
      when(mockErsUtil.withArticle(ArgumentMatchers.eq("CSOP"))).thenReturn("a CSOP")
      when(mockErsUtil.withArticle(ArgumentMatchers.eq("EMI"))).thenReturn("an EMI")

      val hc: HeaderCarrier = HeaderCarrier()
      val invalidSheet: ERSFileProcessingException = intercept[ERSFileProcessingException]{
        identifyAndDefineSheet("CSOP_OptionsExercised_V3","emi")(hc, implicitly[Messages])
      }
      invalidSheet.message mustBe "ers.exceptions.dataParser.incorrectSchemeType"
      invalidSheet.optionalParams mustBe Seq("a CSOP", "an EMI", "CSOP_OptionsExercised_V3")
    }

    "isBlankRow" in new DataGeneratorObj("CSOP") {
      val testAct = List("","","","")
      isBlankRow(testAct) must be (true)
      val testAct1 = List("dfgdg","","","")
      isBlankRow(testAct1) must be (false)
    }

    "get an exception if ods file has less than 9 rows and doesn't have header data" in new DataGeneratorObj("CSOP") {
      val result: ERSFileProcessingException = intercept[ERSFileProcessingException] {
        getErrors(XMLTestData.getInvalidCSOPWithoutHeaders,"csop","CSOP.ods")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      }
      result.message mustBe "ers.exceptions.dataParser.incorrectHeader"
      result.optionalParams mustBe Seq("CSOP_OptionsGranted_V3", "CSOP.ods")
    }

    "get an exception if ods file has more than 1 sheet but 1 of the sheets has less than 9 rows and doesn't have header data" in new DataGeneratorObj("CSOP") {
      val result: ERSFileProcessingException = intercept[ERSFileProcessingException] {
        getErrors(XMLTestData.getInvalidCSOPWith2Sheets1WithoutHeaders,"csop","CSOP.ods")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      }
      result.message mustBe "ers.exceptions.dataParser.incorrectHeader"
      result.optionalParams mustBe Seq("CSOP_OptionsGranted_V3", "CSOP.ods")
    }

    "get an exception if ods file doesn't contain any data" in new DataGeneratorObj("CSOP") {
      val result: ERSFileProcessingException = intercept[ERSFileProcessingException] {
        getErrors(XMLTestData.getCSOPWithoutData,"csop","CSOP.ods")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      }
      result.message mustBe "ers.exceptions.dataParser.noData"
      result.optionalParams mustBe Seq.empty[String]
    }

    "get no errors for EMI" in new DataGeneratorObj("EMI") {
      val result = getErrors(XMLTestData.getEMIAdjustmentsTemplate,"emi","")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      result.foreach(_.errors.size mustBe 0)
    }

    "collect errors in the first sheet of EMI" in new DataGeneratorObj("EMI") {
      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(Some(List(ValidationError(Cell("A", 1, "badValue"), "rule", "error", "errorMessage"))))
        .thenReturn(None)
      val result = getErrors(XMLTestData.getInvalidEMIAdjustmentsTemplate,"emi","")(
        hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      result.head.errors.size mustBe 1
    }

    "collect errors in second sheet of EMI" in new DataGeneratorObj("EMI") {
      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(None)
        .thenReturn(Some(List(
            ValidationError(Cell("A", 1, "badValue"), "rule", "error", "errorMessage")
          )))
        .thenReturn(None)
      val result = getErrors(XMLTestData.getEMIAdjustmentsTemplate ++ XMLTestData.getInvalidEMIReplacedTemplate,"emi","")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      result(1).errors.size mustBe 1
    }

    "expand repeated rows and report correct error row" in new DataGeneratorObj("EMI") {
      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(Some(List(ValidationError(Cell("A", 13, "badValue"), "rule", "error", "errorMessage"))))
        .thenReturn(None)
      val result = getErrors(XMLTestData.getInvalidEMIWithRepeats,"emi","")(hc = HeaderCarrier(),Fixtures.buildEmpRefRequestWithSessionId("GET"), implicitly[Messages])
      result.head.errors.size mustBe 1
      result.head.errors.head.cell.row mustBe 13
    }

  }

  "setValidatorCsv" must {
    val mockErsValidationConfigs: ERSValidationConfigs = mock[ERSValidationConfigs]
    class DataGeneratorCsv extends DataGenerator(mockAuditEvents, mockMetrics, testParserUtil, mockErsValidationConfigs, mockErsUtil, mockErsValidator)

    "return a Right with the validator when receiving happy response from tabular-data-validator" in new DataGeneratorCsv {
      val returnValidator: DataValidator = mock[DataValidator]
      when(mockErsValidationConfigs.getValidator(any())).thenReturn(returnValidator)

      setValidatorCsv("CSOP_OptionsGranted_V3") mustBe Right(returnValidator)
    }

    "return a Failure with the exception when receiving an exception" in {
      val thrownException = new RuntimeException("this is bad")
      val testDataGen = new DataGenerator(mockAuditEvents, mockMetrics, testParserUtil, mockErsValidationConfigs, mockErsUtil, mockErsValidator)
      when(mockErsValidationConfigs.getValidator(any())).thenThrow(thrownException)
      val failedValue: Either[Throwable, DataValidator] = testDataGen.setValidatorCsv("CSOP_OptionsGranted_V3")

      val returnedExceptionExample: ERSFileProcessingException = ERSFileProcessingException(
        "ers.exceptions.dataParser.configFailure",
        Messages("ers.exceptions.dataParser.validatorError"),
        optionalParams = Seq("CSOP_OptionsGranted_V3")
      )

      assert(failedValue.isLeft)
      assert(failedValue.left.get.isInstanceOf[ERSFileProcessingException])
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].context mustEqual returnedExceptionExample.context
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].message mustEqual returnedExceptionExample.message
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].optionalParams mustEqual returnedExceptionExample.optionalParams

      verify(mockAuditEvents, times(1))
        .auditRunTimeError(
          ArgumentMatchers.eq(thrownException),
          ArgumentMatchers.eq("Could not set the validator"),
          ArgumentMatchers.eq("CSOP_OptionsGranted_V3"))(any(), any())
    }
  }

  "identifyAndDefineSheetEither" must {
    "return Right with the sheet name if given valid input" in new DataGeneratorObj("CSOP") {
      identifyAndDefineSheetCsv((SheetInfo("someInput",1,"anInput","","",List("")), "SOMEINPUT")) mustBe Right("anInput")
    }

    "return Left with a processing exception if given invalid input" in {
      when(mockErsUtil.withArticle(any())).thenReturn("ersUtilReturn")
      val testDataGen: DataGenerator = new DataGenerator(mockAuditEvents, mockMetrics, testParserUtil, testErsValidationConfigs, mockErsUtil, mockErsValidator)

      val failedValue: Either[Throwable, String] = testDataGen
        .identifyAndDefineSheetCsv((SheetInfo("differentInput",1,"aSheetName","","",List("")), "SOMEINPUT"))
      val returnedExceptionExample: ERSFileProcessingException = ERSFileProcessingException(
        "ers.exceptions.dataParser.incorrectSchemeType",
        Messages("ers.exceptions.dataParser.incorrectSchemeType", "differentinput", "someinput"),
        optionalParams = Seq("ersUtilReturn", "ersUtilReturn", "aSheetName")
      )

      assert(failedValue.isLeft)
      assert(failedValue.left.get.isInstanceOf[ERSFileProcessingException])
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].context mustEqual returnedExceptionExample.context
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].message mustEqual returnedExceptionExample.message
      failedValue.left.get.asInstanceOf[ERSFileProcessingException].optionalParams mustEqual returnedExceptionExample.optionalParams

      verify(mockAuditEvents, times(1)).fileProcessingErrorAudit("differentInput", "aSheetName", "differentinput is not equal to someinput")


    }
  }

  "getSheetCsv" must {

    "return Right with a tuple of sheetInfo on uploadedfile and the selected scheme name when they match" in new DataGeneratorObj("CSOP") {
      getSheetCsv("CSOP_OptionsGranted_V3", "CSOP") mustBe Right((
        SheetInfo(csop,  1, csopSheet1Name , csopSheet1title, csopSheet1ValConfig,  csopOptionsGrantedHeaderRow),
        "CSOP"
        )
      )
    }

    "return Left with an ERSFileProcessingException if the uploaded file name isn't found" in new DataGeneratorObj("CSOP") {
      getSheetCsv("thisIsInvalid", "CSOP") mustBe Left(ERSFileProcessingException(
        "ers.exceptions.dataParser.incorrectSheetName",
        Messages("ers.exceptions.dataParser.unidentifiableSheetName") + " thisIsInvalid",
        needsExtendedInstructions = true,
        optionalParams = Seq("thisIsInvalid", "CSOP")))
    }
  }
}
