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

package utils

import helpers.ErsTestHelper
import models.SheetErrors
import org.mockito.Mockito
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer

class CsvParserUtilSpec extends UnitSpec with ErsTestHelper {
  def parserUtil: CsvParserUtil = new CsvParserUtil(mockAppConfig)

  "formatDataToValidate" must {
    "truncate array depending on number of columns in given sheet" in {
      val rowData: Array[String] = Array.fill(50)("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V3")
      result.length shouldBe 4
    }

    "add empty strings to match number of columns in given sheet" in {
      val rowData: Array[String] = Array("")
      val result = parserUtil.formatDataToValidate(rowData, "Other_Grants_V3")
      result.length shouldBe 4
    }
  }

  "getSheetErrors" must {
    "return valid amount of sheetErrors if errorCount is present in the appConfig" in {
      Mockito.when(mockAppConfig.errorCount).thenReturn(Some(1))

      val validationError: ValidationError = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val testSheetErrors: SheetErrors = SheetErrors("aName", new ListBuffer() ++ (1 to 100).map(_ => validationError))
      parserUtil.getSheetErrors(testSheetErrors) shouldBe SheetErrors("aName", ListBuffer(validationError))
    }

    "return 100 validation errors if errorCount is present in the appConfig" in {
      Mockito.when(mockAppConfig.errorCount).thenReturn(None)

      val validationError: ValidationError = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
      val testSheetErrors: SheetErrors = SheetErrors("aName", new ListBuffer() ++ (1 to 200).map(_ => validationError))
      parserUtil.getSheetErrors(testSheetErrors).errors.length shouldBe 100
    }
  }

}
