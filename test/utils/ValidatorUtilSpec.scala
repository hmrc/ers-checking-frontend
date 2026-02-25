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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.ods.SheetErrors

import scala.collection.mutable.ListBuffer

class ValidatorUtilSpec extends AnyWordSpecLike with Matchers {

  val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

  val sheetWithThreeErrors = new ListBuffer[ValidationError].addAll(Seq(list1, list2, list3))
  val sheetWithOneError = new ListBuffer[ValidationError].addOne(list1)

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

      val result = ValidationUtil.getSheetErrors(sheetWithMultipleSchemeError, 100)

      result.head.errors.size shouldBe 3
      result(1).errors.size shouldBe 3
      result(2).errors.size shouldBe 1
    }
  }

  "calling isValid" should {

    "return false if there are no errors in any of the sheetErrors parsed in" in {
      assert(!ValidationUtil.isValid(sheetWithMultipleSchemeError))
    }

    "return true if there are no errors in any of the sheetErrors parsed in" in {
      assert(ValidationUtil.isValid(sheetWithNoSchemeError))
    }

  }

}
