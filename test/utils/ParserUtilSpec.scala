/*
 * Copyright 2018 HM Revenue & Customs
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

import models.SheetErrors
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{Cell, ValidationError}

import scala.collection.mutable.ListBuffer

class ParserUtilSpec extends UnitSpec with MockitoSugar with OneAppPerSuite{

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("metrics.enabled" -> false))
    .build()

  "getDataToValidate" must {

    "truncate array depending on number of columns in given sheet" in {
      val rowData: Array[String] = Array.fill(50)("")
      val result = ParserUtil.formatDataToValidate(rowData, "Other_Grants_V3")
      result.length shouldBe 4
    }

    "add empty strings to match number of columns in given sheet" in {
      val rowData: Array[String] = Array("")
      val result = ParserUtil.formatDataToValidate(rowData, "Other_Grants_V3")
      result.length shouldBe 4
    }
  }

  "calling getSheetErrors" should {

    def buildParserUtil = new ParserUtil {
      override val cacheUtil: CacheUtil = mock[CacheUtil]
    }

    val schemeErrors = new ListBuffer[SheetErrors]()

    val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

    val sheetErrors3 = new ListBuffer[ValidationError]()
    sheetErrors3 += list1
    sheetErrors3 += list2
    sheetErrors3 += list3
    schemeErrors += SheetErrors("sheet_tab_1",sheetErrors3)
    schemeErrors += SheetErrors("sheet_tab_2",sheetErrors3)

    val sheetErrors1 = new ListBuffer[ValidationError]()
    sheetErrors1 += list1
    schemeErrors += SheetErrors("sheet_tab_3",sheetErrors1)

    "return up to the first 100 errors of each sheet" in {

      val result = buildParserUtil.getSheetErrors(schemeErrors)

      result(0).errors.size shouldBe 3
      result(1).errors.size shouldBe 3
      result(2).errors.size shouldBe 1
    }
  }

  "validating the scheme" should {

    def buildParserUtil = new ParserUtil {
      override val cacheUtil: CacheUtil = mock[CacheUtil]
    }

    val error = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val errors = new ListBuffer[ValidationError]
    errors ++= List(error)

    val invalidSheet = SheetErrors ("", errors)
    val validSheet = SheetErrors ("", new ListBuffer())

    "return true if no errors are found" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += validSheet
      schemeErrors += validSheet

      buildParserUtil.isValid(schemeErrors) shouldBe true
    }

    "return false if errors are found in first sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += invalidSheet
      schemeErrors += validSheet
      schemeErrors += validSheet

      buildParserUtil.isValid(schemeErrors) shouldBe false
    }

    "return false if errors are found in second sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += invalidSheet
      schemeErrors += validSheet

      buildParserUtil.isValid(schemeErrors) shouldBe false
    }

    "return false if errors are found in third sheet" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      schemeErrors += validSheet
      schemeErrors += validSheet
      schemeErrors += invalidSheet

      buildParserUtil.isValid(schemeErrors) shouldBe false
    }

  }

  "getTotalErrorCount" should {

    def buildParserUtil = new ParserUtil {
      override val cacheUtil: CacheUtil = mock[CacheUtil]
    }

    "return the total errors found over all sheets" in {
      val schemeErrors = new ListBuffer[SheetErrors]()

      val error = ValidationError(Cell("A",1,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'.")
      val errors = new ListBuffer[ValidationError]
      errors ++= List(error)

      val invalidSheet = SheetErrors ("", errors)
      val validSheet = SheetErrors ("", new ListBuffer())

      schemeErrors += invalidSheet
      schemeErrors += validSheet
      schemeErrors += invalidSheet

      buildParserUtil.getTotalErrorCount(schemeErrors) shouldBe 2
    }

  }

}
