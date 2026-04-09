/*
 * Copyright 2026 HM Revenue & Customs
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
import controllers.auth.{PAYEDetails, RequestWithOptionalEmpRefAndPAYE}
import helpers.ErsTestHelper
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.ProcessOdsService._
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.validator._
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.ods.SheetErrors

import java.io.InputStream
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ProcessOdsServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with ScalaFutures
    with MongoSupport {

  implicit val fakeRequest: RequestWithOptionalEmpRefAndPAYE[AnyContent] = RequestWithOptionalEmpRefAndPAYE(
    FakeRequest(),
    None,
    PAYEDetails(isAgent = false, agentHasPAYEEnrollement = false, None, mockAppConfig)
  )

  def buildProcessOdsService(sheetErrors: ListBuffer[SheetErrors]): ProcessOdsService =
    new ProcessOdsService(mockSessionCacheRepo, mockErsUtil) {
      override def validateOdsFile(fileName: String, processor: InputStream, scheme: String): ListBuffer[SheetErrors] =
        sheetErrors
    }

  "calling performOdsUpload" should {

    val sheetErrors: ListBuffer[SheetErrors] = Fixtures.buildSheetErrors

    "return false if the file has validation errors" in {
      // mocking calls to cache
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))
      when(
        mockSessionCacheRepo.cache[Long](ArgumentMatchers.eq(mockErsUtil.SCHEME_ERROR_COUNT_CACHE), any())(any(), any())
      )
        .thenReturn(Future.successful(("", "")))
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.ERROR_LIST_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val output: Boolean = buildProcessOdsService(sheetErrors)
        .performOdsUpload(10, "testFileName.ods", mockInputStream, "csop")
        .futureValue
      output shouldBe false
    }

    "return true if the file doesn't have any errors" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val emptyErrors     = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      val output: Boolean = buildProcessOdsService(emptyErrors)
        .performOdsUpload(10, "testFileName.ods", mockInputStream, "csop")
        .futureValue

      output shouldBe true
    }

    "return a NoSuchElementException if nothing was found in the cache" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException))

      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))

      val result: Future[Boolean] =
        buildProcessOdsService(emptyErrors).performOdsUpload(10, "testFileName.ods", mockInputStream, "csop")
      intercept[NoSuchElementException](Await.result(result, Duration.Inf))
    }
  }

  val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

  val sheetWithThreeErrors = new ListBuffer[ValidationError].addAll(Seq(list1, list2, list3))
  val sheetWithOneError    = new ListBuffer[ValidationError].addOne(list1)

  val sheetWithMultipleSchemeError = new ListBuffer[SheetErrors].addAll(
    Seq(
      SheetErrors("sheet_tab_1", sheetWithThreeErrors),
      SheetErrors("sheet_tab_2", sheetWithThreeErrors),
      SheetErrors("sheet_tab_3", sheetWithOneError)
    )
  )

  val sheetWithNoSchemeError = new ListBuffer[SheetErrors].addAll(
    Seq(
      SheetErrors("sheet_tab_1", new ListBuffer[ValidationError]()),
      SheetErrors("sheet_tab_2", new ListBuffer[ValidationError]()),
      SheetErrors("sheet_tab_3", new ListBuffer[ValidationError]())
    )
  )

  "calling getSheetErrors" should {

    "return up to the first 100 errors of each sheet" in {

      val result = getSheetErrors(sheetWithMultipleSchemeError, 100)

      result.head.errors.size shouldBe 3
      result(1).errors.size   shouldBe 3
      result(2).errors.size   shouldBe 1
    }
  }

  "calling isValid" should {

    "return false if there are no errors in any of the sheetErrors passed in" in
      assert(!isValid(sheetWithMultipleSchemeError))

    "return true if there are no errors in any of the sheetErrors passed in" in
      assert(isValid(sheetWithNoSchemeError))

  }

  val processOdsService: ProcessOdsService = new ProcessOdsService(mockSessionCacheRepo, mockErsUtil)

  "ProcessOdsService" should {

    "when calling the ers-file-validator-config library directly" should {

      "must successfully process valid EMI ODS data" in {
        val sheetErrors: ListBuffer[SheetErrors] =
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateLarge, "EMI")
        sheetErrors should contain theSameElementsAs ListBuffer(SheetErrors("EMI40_Adjustments_V4", ListBuffer()))
      }

      "must return DataContainsAmpersandException when ODS data contains ampersands" in {
        val exception: DataContainsAmpersandException = intercept[DataContainsAmpersandException](
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateWithAmpersand, "EMI")
        )
        exception.getMessage() shouldBe "Must not contain ampersands."
      }

      "must return NoDataException when ODS data is empty" in {
        val exception: NoDataException = intercept[NoDataException](
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateWithNoData, "EMI")
        )

        exception.getMessage() shouldBe "No data in file"
      }

      "must return IncorrectHeaderException when ODS header is invalid" in {

        val exception: IncorrectHeaderException = intercept[IncorrectHeaderException](
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateWithIncorrectHeader, "EMI")
        )

        exception.getMessage() shouldBe "Incorrect header row"
      }

      "must return the expected sheetErrors when ODS data is invalid" in {

        val sheetErrors: ListBuffer[SheetErrors] =
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateWithInvalidData, "EMI")

        val expectedSheetErrors: SheetErrors = SheetErrors(
          "EMI40_Adjustments_V4",
          ListBuffer(
            ValidationError(Cell("A", 10, "123"), "error.1", "001", "Enter 'yes' or 'no'"),
            ValidationError(Cell("B", 10, "123"), "error.2", "002", "Enter 'yes' or 'no'"),
            ValidationError(Cell("C", 10, "123"), "error.3", "003", "Enter 'yes' or 'no'"),
            ValidationError(Cell("D", 10, "canoe"), "error.4", "004", "Enter '1', '2', '3', '4', '5', '6', '7' or '8'"),
            ValidationError(
              Cell("E", 10, "apples"),
              "error.5",
              "005",
              "Enter a date that matches the yyyy-mm-dd pattern"
            )
          )
        )

        sheetErrors should contain theSameElementsAs ListBuffer(expectedSheetErrors)
      }

      "must return IncorrectSheetNameException when ODS sheet name is unknown" in {

        val exception: IncorrectSheetNameException = intercept[IncorrectSheetNameException](
          processOdsService.validateOdsFile(
            "EMI.ods",
            XMLTestData.getEMIAdjustmentsTemplateWithIncorrectSheetName,
            "EMI"
          )
        )

        exception.getMessage() shouldBe "Incorrect sheet name"
      }

      "must return IncorrectSchemeException when ODS sheet belongs to a different scheme type" in {

        val exception: IncorrectSchemeException = intercept[IncorrectSchemeException](
          processOdsService.validateOdsFile("EMI.ods", XMLTestData.getEMIAdjustmentsTemplateWithCsopSchemeType, "EMI")
        )

        exception.getMessage() shouldBe "Incorrect Scheme type"
      }
    }
  }

}
