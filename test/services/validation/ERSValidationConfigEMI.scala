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
import uk.gov.hmrc.services.validation.{Cell, DataValidator, Row, ValidationError}
import org.scalatestplus.play.PlaySpec

import services.validation.EMITestData.{ERSValidationEMIRLCTestData, ERSValidationEMINonTaxableTestData, ERSValidationEMIReplacedTestData, ERSValidationEMITaxableTestData, ERSValidationEMIAdjustmentsTestData}


class EMIAdjustmentsV3ValidationTest extends PlaySpec with ERSValidationEMIAdjustmentsTestData with ValidationTestRunner {

  "ERS Validation tests for EMI Adjustments" should {
    val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-adjustments-validation-config"))
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when Column A is answered yes, column B is a mandatory field" in {
      val cellB = Cell("B", rowNumber, "")
      val cellA = Cell("A", rowNumber, "yes")
      val row = Row(1,Seq(cellB,cellA))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellB,"mandatoryB","B01","'2. Has there been a change to the description of the shares under option? (yes/no)' must be answered if '1. Has there been any adjustment of options following a variation in the share capital of the company? (yes/no)' was answered with YES.")
      ))
    }

    "when Column B is answered yes, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1,Seq(cellC,cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC,"mandatoryC","C01","'3. Is the adjustment a disqualifying event? (yes/no). If YES go to question 4. If NO go to question 5.' must be answered if '2. Has there been a change to the description of the shares under option? (yes/no)' was answered with YES.")
      ))
    }

    "when Column C is answered yes, column D is a mandatory field" in {
      val cellD = Cell("D", rowNumber, "")
      val cellC = Cell("C", rowNumber, "yes")
      val row = Row(1,Seq(cellD,cellC))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellD,"mandatoryD","D01","'4. If yes, enter a number from 1 to 8 depending on the nature of the disqualifying event. Follow the link at cell A7 for a list of disqualifying events' must be answered if '3. Is the adjustment a disqualifying event? (yes/no). If YES go to question 4. If NO go to question 5.' was answered with YES.")
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

}

class EMIReplacedV3ValidationTest extends PlaySpec with ERSValidationEMIReplacedTestData with ValidationTestRunner{

  "ERS EMI Replaced Validation Test" should {
    val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-replaced-validation-config"))
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

}
class EMIRLCV3ValidationTest extends PlaySpec with ERSValidationEMIRLCTestData with ValidationTestRunner{


  "ERS EMI RLC Validation Test" should {
    val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-rlc-validation-config"))
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when Column K is answered yes, column L is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1, Seq(cellC, cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC, "mandatoryC", "C01", "'3. If yes, enter a number from 1 to 8 depending on the nature of the disqualifying event.Follow the link at cell A7 for a list of disqualifying events' must be answered if '2. Is the release, lapse or cancellation the result of a disqualifying event?(yes/no)' was answered with YES.")
      ))
    }
    "when Column J is answered yes, column K is a mandatory field" in {
      val cellK = Cell("K", rowNumber, "")
      val cellJ = Cell("J", rowNumber, "yes")
      val row = Row(1, Seq(cellK, cellJ))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellK, "mandatoryK", "K01", "'11. If yes enter the amount£e.g. 10.1234' must be answered if '10. Was money or value received?(yes/no)If yes go to question 11, otherwise no more information is needed for this event.' was answered with YES.")
      ))
    }
    "when Column K is answered, column L is a mandatory field" in {
      val cellL = Cell("L", rowNumber, "")
      val cellK = Cell("K", rowNumber, "10.1234")
      val row = Row(1, Seq(cellL, cellK))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellL, "mandatoryL", "L01", "'12. PAYE operated?(yes/no)' must be answered if '11. If yes enter the amount£e.g. 10.1234' was answered.")
      ))
    }
  }
}

class EMINonTaxableV3ValidationTest extends PlaySpec with ERSValidationEMINonTaxableTestData with ValidationTestRunner{

  "ERS EMI Replaced Exercised Validation Test" should {

    val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-nontaxable-validation-config"))
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when Column K is answered no, column L is a mandatory field" in {
      val cellL = Cell("L", rowNumber, "")
      val cellK = Cell("K", rowNumber, "no")
      val row = Row(1,Seq(cellL,cellK))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellL,"mandatoryL","L01","'12. If no, was the market value agreed with HMRC? (yes/no)' must be answered if '11. Are the shares subject to the option exercised listed on a recognised stock exchange? (yes/no)' was answered with NO.")
      ))
    }

    "when Column L is answered yes, column M is a mandatory field" in {
      val cellM = Cell("M", rowNumber, "")
      val cellL = Cell("L", rowNumber, "yes")
      val row = Row(1,Seq(cellM,cellL))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellM,"mandatoryM","M01","'13. If yes, enter the HMRC reference given' must be answered if '12. If no, was the market value agreed with HMRC? (yes/no)' was answered with YES.")
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

}


class EMITaxableV3ValidationTest extends PlaySpec with ERSValidationEMITaxableTestData with ValidationTestRunner {

  "ERS Validation tests for EMI Taxable" should {

    val validator = DataValidator(ConfigFactory.load.getConfig("ers-emi-taxable-validation-config"))
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when Column B is answered yes, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1,Seq(cellC,cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC,"mandatoryC","C01","'3. If yes, enter a number from 1 to 8 depending on the nature of the disqualifying event. Follow the link at cell A7 for a list of disqualifying events' must be answered if '2. Is this as a result of a disqualifying event? (yes/no)' was answered with YES.")
      ))
    }

    "when Column O is answered yes, column P is a mandatory field" in {
      val cellP = Cell("P", rowNumber, "")
      val cellO = Cell("O", rowNumber, "no")
      val row = Row(1,Seq(cellP,cellO))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellP,"mandatoryP","P01","'16. Has the market value been agreed with HMRC? (yes/no)' must be answered if '15. Is the company listed on a recognised stock exchange? (yes/no)If yes go to question 18If no go to question 16' was answered with NO.")
      ))
    }

    "when Column P is answered yes, column Q is a mandatory field" in {
      val cellQ = Cell("Q", rowNumber, "")
      val cellP = Cell("P", rowNumber, "yes")
      val row = Row(1,Seq(cellQ,cellP))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellQ,"mandatoryQ","Q01","'17. If yes, enter the HMRC reference given' must be answered if '16. Has the market value been agreed with HMRC? (yes/no)' was answered with YES.")
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

}
