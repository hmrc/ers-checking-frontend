/*
 * Copyright 2016 HM Revenue & Customs
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
import hmrc.gsi.gov.uk.services.validation.{Cell, DataValidator, Row, ValidationError}
import org.scalatestplus.play.PlaySpec
import services.validation.SIPTestData.{ERSValidationSIPAwardsTestData, ERSValidationSIPOutTestData}

class SIPAwardsV3ValidationTest extends PlaySpec with ERSValidationSIPAwardsTestData with ValidationTestRunner{

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-sip-awards-validation-config"))

  "Ers Validation tests for SIP Awards" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when awards Column C is answered with 2, column D is a mandatory field" in {
      val cellD = Cell("D", rowNumber, "")
      val cellC = Cell("C", rowNumber, "2")
      val row = Row(1,Seq(cellD,cellC))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellD,"mandatoryD","D01","'4. If free shares, are performance conditions attached to their award? (yes/no)' must be answered if '3. Type of shares awarded Enter a number from 1 to 4 depending on the type of share awarded. Follow the link at cell B10 for a list of the types of share which can be awarded' was answered with 2.")
      ))
    }

    "when awards Column C is answered 1, column E is a mandatory field" in {
      val cellE = Cell("E", rowNumber, "")
      val cellC= Cell("C", rowNumber, "1")
      val row = Row(1, Seq(cellE, cellC))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellE, "mandatoryE", "E01", "'5. If matching shares, what is the ratio of shares to partnership shares? Enter ratio for example 2:1; 2/1' must be answered if '3. Type of shares awarded Enter a number from 1 to 4 depending on the type of share awarded. Follow the link at cell B10 for a list of the types of share which can be awarded' was answered with 1.")
      ))
    }

    "when awards Column O is answered NO, column P is a mandatory field" in {
      val cellP = Cell("P", rowNumber, "")
      val cellO= Cell("O", rowNumber, "NO")
      val row = Row(1, Seq(cellP, cellO))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellP, "mandatoryP", "P01", "'16. If no, was the market value agreed with HMRC? (yes/no)' must be answered if '15. Are the shares listed on a recognised stock exchange? (yes/no)' was answered with NO.")
      ))
    }

    "when awards Column P is answered YES, column Q is a mandatory field" in {
      val cellQ = Cell("Q", rowNumber, "")
      val cellP= Cell("P", rowNumber, "YES")
      val row = Row(1, Seq(cellQ, cellP))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellQ, "mandatoryQ", "Q01", "'17. If yes, enter the HMRC reference given' must be answered if '16. If no, was the market value agreed with HMRC? (yes/no)' was answered with YES.")
      ))
    }


  }
  "when a valid row of data is provided, no ValidationErrors should be raised" in {
    val row = Row(1,getValidRowData)
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe None
  }

  "when a invalid row of data is provided, a list of ValidationErrors should be raised" in {
    val row = Row(1,getInvalidRowData)
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt.get.size mustBe getInvalidRowData.size
  }


}
class SIPOutV3ValidationTest extends PlaySpec with ERSValidationSIPOutTestData with ValidationTestRunner{

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-sip-out-validation-config"))

  "Ers Validation tests for SIP Out" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

  "when awards Column O is answered NO, column P is a mandatory field" in {
    val cellP = Cell("P", rowNumber, "")
    val cellO= Cell("O", rowNumber, "NO")
    val row = Row(1, Seq(cellP, cellO))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellP, "mandatoryP", "P01", "'16. If no, for other than dividend shares, was PAYE operated? (yes/no)' must be answered if '15. Have all the shares been held in the plan for 5 years or more at the date they ceased to be part of the plan? (yes/no) If yes, no more information is needed for this event. If no, go to question 16' was answered with NO.")
    ))
  }

  "when awards Column P is answered NO, column Q is a mandatory field" in {
    val cellQ = Cell("Q", rowNumber, "")
    val cellP= Cell("P", rowNumber, "NO")
    val row = Row(1, Seq(cellQ, cellP))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellQ, "mandatoryQ", "Q01", "'17. If no, does this withdrawal of shares qualify for tax relief? (yes/no)' must be answered if '16. If no, for other than dividend shares, was PAYE operated? (yes/no)' was answered with NO.")
    ))
  }

  "when a valid row of data is provided, no ValidationErrors should be raised" in {
    val row = Row(1,getValidRowData)
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe None
  }

  "when a invalid row of data is provided, a list of ValidationErrors should be raised" in {
    val row = Row(1,getInvalidRowData)
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt.get.size mustBe getInvalidRowData.size
  }
}
