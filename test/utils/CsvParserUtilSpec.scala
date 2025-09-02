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
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer

class CsvParserUtilSpec extends AnyWordSpecLike with Matchers with OptionValues with ErsTestHelper {

  def parserUtil: CsvParserUtil = new CsvParserUtil(mockAppConfig)

  "formatDataToValidate" must {
    "truncate array depending on number of columns in given sheet" in {
      val rowData: Seq[String] = Seq.fill(50)("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V4")
      result.length shouldBe 4
    }

    "pass the columns on if there's fewer than in given sheet" in {
      val rowData: Seq[String] = Seq("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V4")
      result.length shouldBe 1
    }
  }

  "getSheetErrors" must {
    "return valid amount of sheetErrors if errorCount is present in the appConfig" in {
      when(mockAppConfig.errorCount).thenReturn(1)
      val validationError: ValidationError = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val testSheetErrors: SheetErrors = SheetErrors("aName", new ListBuffer() ++ (1 to 100).map(_ => validationError))
      parserUtil.getSheetErrors(testSheetErrors) shouldBe SheetErrors("aName", ListBuffer(validationError))
    }

    "return 100 validation errors if errorCount is present in the appConfig" in {
      when(mockAppConfig.errorCount).thenReturn(100)
      val validationError: ValidationError = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val testSheetErrors: SheetErrors = SheetErrors("aName", new ListBuffer() ++ (1 to 200).map(_ => validationError))
      parserUtil.getSheetErrors(testSheetErrors).errors.length shouldBe 100
    }
  }

  "rowIsEmpty" must {
    "return false if the row has more than one entry" in {
      val list = List("col1", "col2")
      parserUtil.rowIsEmpty(list) shouldBe false
    }

    "return true if the row is an empty list" in {
      val list = List.empty
      parserUtil.rowIsEmpty(list) shouldBe true
    }

    "return true if the row is a list with one item with only spaces in it" in {
      val list = List("      ")
      parserUtil.rowIsEmpty(list) shouldBe true
    }

    "return false if the row is a list with one valid item in it" in {
      val list = List("i am not empty")
      parserUtil.rowIsEmpty(list) shouldBe false
    }
  }
}
