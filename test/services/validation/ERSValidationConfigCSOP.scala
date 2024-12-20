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

package services.validation

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import services.validation.CSOPTestData.{ERSValidationCSOPExercisedTestData, ERSValidationCSOPGrantedTestData, ERSValidationCSOPRCLTestData}
import services.validation.ValidationErrorHelper._
import uk.gov.hmrc.services.validation.DataValidator
import uk.gov.hmrc.services.validation.models.{Cell, Row, ValidationError}

class CSOPOptionsGrantedCSOPTest extends PlaySpec with ERSValidationCSOPGrantedTestData with ValidationTestRunner {

  "ERS CSOP Granted V4 Validation tests" should {
    val validator = new DataValidator(ConfigFactory.load.getConfig("ers-csop-granted-validation-config"))
    runValidationTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when sharesListedOnSE is answered no, mvAgreedHMRC is a mandatory field" in {
      val cellG = Cell("G", rowNumber, "")
      val cellF = Cell("F", rowNumber, "no")
      val row = Row(1,Seq(cellG,cellF))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
        ValidationError(cellG,"mandatoryG","G01","Enter ‘yes’ or ‘no’")
    }

    "when sharesListedOnSE is answered yes, hmrcRef is a mandatory field" in {
      val cellH = Cell("H", rowNumber, "")
      val cellG = Cell("G", rowNumber, "yes")
      val row = Row(1,Seq(cellH,cellG))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
        ValidationError(cellH,"mandatoryH","G02","Enter the HMRC reference (must be less than 11 characters)")
    }

    "when a valid row of data is provided, no ValidationErrors should be raised" in {
      val row = Row(1,getValidRowData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages mustBe None
    }

    "when a invalid row of data is provided, a list of ValidationErrors should be raised" in {
      val row = Row(1,getInvalidRowData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.get.size mustBe 7
    }
  }

  "ERS CSOP Granted V5 Validation tests" should {
    val validator = new DataValidator(ConfigFactory.load.getConfig("ers-csop-granted-validation-config-v5"))
    runValidationTests(validator, getDescriptionsV5, getTestData, getExpectedResults)

    "when sharesListedOnSE is answered no, mvAgreedHMRC is a mandatory field" in {
      val cellG = Cell("G", rowNumber, "")
      val cellF = Cell("F", rowNumber, "no")
      val row = Row(1,Seq(cellG,cellF))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
      ValidationError(cellG,"mandatoryG","G01","Enter ‘yes’ or ‘no’")
    }

    "when sharesListedOnSE is answered yes, hmrcRef is a mandatory field" in {
      val cellH = Cell("H", rowNumber, "")
      val cellG = Cell("G", rowNumber, "yes")
      val row = Row(1,Seq(cellH,cellG))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
      ValidationError(cellH,"mandatoryH","G02","Enter the HMRC reference (must be less than 11 characters)")
    }

    "when a valid row of data is provided, no ValidationErrors should be raised" in {
      val row = Row(1,getValidRowData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages mustBe None
    }

    "when a invalid row of data is provided, a list of ValidationErrors should be raised" in {
      val row = Row(1,getInvalidRowData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.get.size mustBe 7
    }
  }
}

class CSOPOptionsRCLTest extends PlaySpec with ERSValidationCSOPRCLTestData with ValidationTestRunner {

  "ERS CSOP Options RCL Validation Test " should {

    val validator = new DataValidator(ConfigFactory.load.getConfig("ers-csop-rcl-validation"))
    runValidationTests(validator, getDescriptions, getTestData, getExpectedResults)
    "when colB is answered yes, colC is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1,Seq(cellB,cellC))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages.get must contain(
        ValidationError(cellC,"mandatoryC","C01","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)")
      )
    }

    "when colB is answered yes then remaining columns from C to I are mandatory and if entered invalid or missing data then it should throw error " in {
      val row = Row(1,getWronglyEnteredCellData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages.get must equal(List(
        ValidationError(Cell("E", rowNumber, ""), "mandatoryE", "E01", "Must be less than 36 characters and can only have letters, numbers, hyphens or apostrophes"),
        ValidationError(Cell("C", rowNumber, "12.444"), "error.3", "003", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)"),
        ValidationError(Cell("D", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"), "error.4", "004", "Enter a first name (must be less than 36 characters and can only have letters, numbers, hyphens or apostrophes)")
      ))

    }

    "when colB is answered yes then remaining columns from C to I are mandatory and if entered valid data then it should not throw any error" in {
      val row = Row(1,getAllCellData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages mustBe None
    }

    "when colB is answered no then remaining columns from C to I are optional " in {
      val row = Row(1,getRequiredCellData)
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      resOpt.withErrorsFromMessages mustBe None
    }
  }
}

class CSOPOptionsExercisedTest extends PlaySpec with ERSValidationCSOPExercisedTestData with ValidationTestRunner {

  "ERS CROP Options Exercised Validation Test" should {
    val validator = new DataValidator(ConfigFactory.load.getConfig("ers-csop-exercised-validation"))
    runValidationTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when mvAgreedHMRC is answered yes, hmrcRef is a mandatory field" in {
      val cellO = Cell("O", rowNumber, "")
      val cellN = Cell("N", rowNumber, "yes")
      val row = Row(1,Seq(cellO,cellN))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
        ValidationError(cellO,"mandatoryO","O01","Enter the HMRC reference (must be less than 11 characters)")
    }

    "when payeOperatedApplied is answered yes, deductibleAmount must be answered" in {
      val cellR = Cell("R", rowNumber, "")
      val cellQ = Cell("Q", rowNumber, "yes")
      val row = Row(1,Seq(cellR,cellQ))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row)
      assert(resOpt.isDefined)
      resOpt.withErrorsFromMessages.get must contain
        ValidationError(cellR,"mandatoryR","R01","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)")
    }
  }
}
