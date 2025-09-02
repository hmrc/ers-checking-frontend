/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import helpers.ErsTestHelper
import models.SheetErrors
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

class ParserUtilSpec extends AnyWordSpecLike with Matchers with OptionValues with ErsTestHelper with ScalaFutures {

  def parserUtil: ParserUtil = new ParserUtil(mockErsUtil, mockAppConfig, mockSessionCacheRepo)

  "getDataToValidate" must {
    "truncate array depending on number of columns in given sheet" in {
      val rowData: Seq[String] = Seq.fill(50)("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V4")
      result.length shouldBe 4
    }

    "add empty strings to match number of columns in given sheet" in {
      val rowData: Seq[String] = Seq("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V4")
      result.length shouldBe 4
    }
  }

  "calling getSheetErrors" should {
    val schemeErrors = new ListBuffer[SheetErrors]()

    val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

    val sheetErrors3 = new ListBuffer[ValidationError]()
    sheetErrors3 += list1
    sheetErrors3 += list2
    sheetErrors3 += list3
    schemeErrors += SheetErrors("sheet_tab_1", sheetErrors3)
    schemeErrors += SheetErrors("sheet_tab_2", sheetErrors3)

    val sheetErrors1 = new ListBuffer[ValidationError]()
    sheetErrors1 += list1
    schemeErrors += SheetErrors("sheet_tab_3", sheetErrors1)

    "return up to the first 100 errors of each sheet" in {

      val result = parserUtil.getSheetErrors(schemeErrors)

      result.head.errors.size shouldBe 3
      result(1).errors.size shouldBe 3
      result(2).errors.size shouldBe 1
    }
  }

  "validating the scheme" should {
    val error = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val errors = new ListBuffer[ValidationError]
    errors ++= List(error)

    val invalidSheet = SheetErrors("", errors)
    val validSheet = SheetErrors("", new ListBuffer())

    "return true if no errors are found" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += validSheet
      schemeErrors += validSheet

      parserUtil.isValid(schemeErrors) shouldBe true
    }

    "return false if errors are found in first sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += invalidSheet
      schemeErrors += validSheet
      schemeErrors += validSheet

      parserUtil.isValid(schemeErrors) shouldBe false
    }

    "return false if errors are found in second sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += invalidSheet
      schemeErrors += validSheet

      parserUtil.isValid(schemeErrors) shouldBe false
    }

    "return false if errors are found in third sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += validSheet
      schemeErrors += invalidSheet

      parserUtil.isValid(schemeErrors) shouldBe false
    }
  }

  "getTotalErrorCount" should {
    "return the total errors found over all sheets" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      val error = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val errors = new ListBuffer[ValidationError]
      errors ++= List(error)

      val invalidSheet = SheetErrors("", errors)
      val validSheet = SheetErrors("", new ListBuffer())

      schemeErrors += invalidSheet
      schemeErrors += validSheet
      schemeErrors += invalidSheet

      parserUtil.getTotalErrorCount(schemeErrors) shouldBe 2
    }
  }

  "isFileValid" should {
    "return a Success(true) if no errors are present" in {
      parserUtil.isFileValid(ListBuffer.empty).futureValue shouldBe Success(true)
    }

    "return a Success(false) if errors are present but no exceptions" in {
      when(mockSessionCacheRepo.cache[Long](any(), any())(any(), any())).thenReturn(Future.successful(("", "")))
      when(mockSessionCacheRepo.cache[ListBuffer[SheetErrors]](any(), any())(any(), any())).thenReturn(Future.successful(("", "")))

      parserUtil.isFileValid(
        ListBuffer(SheetErrors("sheet", ListBuffer(ValidationError(Cell("A", 1, "cell"), "ruleId", "errorId", "errorMsg"))))
      ).futureValue shouldBe Success(false)
    }

    "return Failure if exception was thrown" in {
      val exception = new RuntimeException("this  is a runtime exception")
      when(mockSessionCacheRepo.cache[Long](any(), any())(any(), any())).thenReturn(Future.failed(exception))

      parserUtil.isFileValid(
        ListBuffer(SheetErrors("sheet", ListBuffer(ValidationError(Cell("A", 1, "cell"), "ruleId", "errorId", "errorMsg"))))
      ).futureValue shouldBe Failure(exception)
    }
  }
}
