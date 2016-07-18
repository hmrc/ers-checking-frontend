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

import services.validation.OTHERTestData._

class OTHERGrantsV3ValidationTest extends PlaySpec with ERSValidationOTHERGrantsTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-grants-validation-config"))

  "ERS Validation tests for OTHER Grants" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
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

class OTHEROptionsV3ValidationTest extends PlaySpec with ERSValidationOTHEROptionsTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-options-validation-config"))

  "ERS Validation tests for OTHER Options" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

  "when options Column B is answered yes, column C is a mandatory field" in {
    val cellC = Cell("C", rowNumber, "")
    val cellB = Cell("B", rowNumber, "yes")
    val row = Row(1,Seq(cellC,cellB))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellC,"mandatoryC","C01","'3. If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' was answered with YES.")
    ))
  }



  "when options Column AL is answered yes, column AM is a mandatory field" in {
    val cellAM = Cell("AM", rowNumber, "")
    val cellAL = Cell("AL", rowNumber, "yes")
    val row = Row(1,Seq(cellAM,cellAL))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellAM,"mandatoryAM","AM01","'39. If yes, amount of money or value received £ e.g. 10.1234' must be answered if '38. If securities were not acquired, was money or value received on the release, assignment, cancellation or lapse of the option? (yes/no) If yes go to next question If no, no further information required on this event.' was answered with YES.")
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

class OTHERAcquisitionV3ValidationTest extends PlaySpec with ERSValidationOTHERAcquisitionTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-acquisition-validation-config"))

  "ERS Validation tests for OTHER Acquisition" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when Column B is answered YES, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1,Seq(cellC,cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC,"mandatoryB","MB","'3. If yes enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' was answered with YES.")
      ))
    }

    "when Column T is answered YES, column U is a mandatory field" in {
      val cellU = Cell("U", rowNumber, "")
      val cellT = Cell("T", rowNumber, "yes")
      val row = Row(1,Seq(cellU,cellT))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellU,"mandatoryT","MT","'21. If the securities are shares, are they listed on a recognised stock exchange? (yes/no) If no go to question 22, If yes go to question 24' must be answered if '20. If the securities are not shares enter ' no' and go to question 24 If the securities are shares, are they part of the largest class of shares in the company? (yes/no)' was answered with YES.")
      ))
    }

    "when Column U is answered NO, column V is a mandatory field" in {
      val cellV = Cell("V", rowNumber, "")
      val cellU = Cell("U", rowNumber, "no")
      val row = Row(1,Seq(cellV,cellU))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellV,"mandatoryU","MU","'22. If shares were not listed on a recognised stock exchange, was valuation agreed with HMRC? (yes/no)' must be answered if '21. If the securities are shares, are they listed on a recognised stock exchange? (yes/no) If no go to question 22, If yes go to question 24' was answered with NO.")
      ))
    }

    "when Column V is answered YES, column W is a mandatory field" in {
      val cellW = Cell("W", rowNumber, "")
      val cellV = Cell("V", rowNumber, "yes")
      val row = Row(1,Seq(cellW,cellV))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellW,"mandatoryV","MV","'23. If yes, enter the HMRC reference given' must be answered if '22. If shares were not listed on a recognised stock exchange, was valuation agreed with HMRC? (yes/no)' was answered with YES.")
      ))
    }

    "when Column Y is filled 1, column Z is a mandatory field" in {
      val cellZ = Cell("Z", rowNumber, "")
      val cellY = Cell("Y", rowNumber, "1")
      val row = Row(1,Seq(cellZ,cellY))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellZ,"mandatoryY","MY","'26. If restricted, nature of restriction. Enter a number from 1-3, follow the link at cell A7 for a list of restrictions' must be answered if '25. Security type. Enter a number from 1 to 3, (follow the link at cell A7 for a list of security types). If restricted go to next question. If convertible go to question 32. If both restricted and convertible enter 1 and answer all questions 26 to 32. If neither restricted nor convertible go to question 29.' was filled with 1.")
      ))
    }

    "when Column Y is filled 1, column AD is a mandatory field" in {
      val cellAD = Cell("AD", rowNumber, "")
      val cellY = Cell("Y", rowNumber, "1")
      val row = Row(1,Seq(cellAD,cellY))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellAD,"mandatoryY2","MY2","'30. If restricted, has an election been operated to disregard restrictions? (yes/no)' must be answered if '25. Security type. Enter a number from 1 to 3, (follow the link at cell A7 for a list of security types). If restricted go to next question. If convertible go to question 32. If both restricted and convertible enter 1 and answer all questions 26 to 32. If neither restricted nor convertible go to question 29.' was filled with 1.")
      ))
    }

    "when Column AD is answered YES, column AE is a mandatory field" in {
      val cellAE = Cell("AE", rowNumber, "")
      val cellAD = Cell("AD", rowNumber, "yes")
      val row = Row(1,Seq(cellAE,cellAD))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellAE,"mandatoryD1","MD1","'31. If an election has been operated to disregard restrictions, have all or some been disregarded? (enter all or some)' must be answered if '30. If restricted, has an election been operated to disregard restrictions? (yes/no)' was answered with YES.")
      ))
    }

    "when Column AI is answered YES, column AJ is a mandatory field" in {
      val cellAJ = Cell("AJ", rowNumber, "")
      val cellAI = Cell("AI", rowNumber, "yes")
      val row = Row(1,Seq(cellAJ,cellAI))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellAJ,"mandatoryI1","MI1","'36. If there was an artificial reduction in value, nature of the artificial reduction Enter a number from 1 to 3. Follow the link in cell A7 for a list of types of artificial restriction' must be answered if '35. Was there an artificial reduction in value on acquisition? (yes/no) If 'yes' go to question 36, if 'No' go to question 37' was answered with YES.")
      ))
    }

    "when Column AK is answered YES, column AL is a mandatory field" in {
      val cellAL = Cell("AL", rowNumber, "")
      val cellAK = Cell("AK", rowNumber, "yes")
      val row = Row(1,Seq(cellAL,cellAK))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellAL,"mandatoryK1","MK1","'38. If shares were acquired under an employee shareholder arrangement, was the total actual market value (AMV) of shares £2,000 or more? (yes/no)' must be answered if '37. Were shares acquired under an employee shareholder arrangement? (yes/no)' was answered with YES.")
      ))
    }

    "when Column T is answered NO, column X is a mandatory field" in {
      val cellX = Cell("X", rowNumber, "")
      val cellT = Cell("T", rowNumber, "no")
      val row = Row(1,Seq(cellX,cellT))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellX,"mandatoryT2","MT2","'24. Number of securities acquired e.g. 100.00' must be answered if '20. If the securities are not shares enter ' no' and go to question 24 If the securities are shares, are they part of the largest class of shares in the company? (yes/no)' was answered with NO.")
      ))
    }

    "when Column U is answered YES, column X is a mandatory field" in {
      val cellX = Cell("X", rowNumber, "")
      val cellU = Cell("U", rowNumber, "yes")
      val row = Row(1,Seq(cellX,cellU))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellX,"mandatoryU2","MU2","'24. Number of securities acquired e.g. 100.00' must be answered if '21. If the securities are shares, are they listed on a recognised stock exchange? (yes/no) If no go to question 22, If yes go to question 24' was answered with YES.")
      ))
    }

    "when Column AI is answered NO, column AK is a mandatory field" in {
      val cellAK = Cell("AK", rowNumber, "")
      val cellAI = Cell("AI", rowNumber, "no")
      val row = Row(1,Seq(cellAK,cellAI))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellAK,"mandatoryI2","MI2","'37. Were shares acquired under an employee shareholder arrangement? (yes/no)' must be answered if '35. Was there an artificial reduction in value on acquisition? (yes/no) If 'yes' go to question 36, if 'No' go to question 37' was answered with NO.")
      ))
    }

  }
}

class OTHERRestrictedSecuritiesV3_ValidationTest extends PlaySpec with ERSValidationOTHERRestrictedSecuritiesTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-restrictedsecurities-validation-config"))

  "ERS Validation tests for OTHER Restricted Securities" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

  "when Column B is answered yes, column C is a mandatory field" in {
    val cellC = Cell("C", rowNumber, "")
    val cellB = Cell("B", rowNumber, "yes")
    val row = Row(1,Seq(cellC,cellB))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellC,"mandatoryC","C01","'3. If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' was answered with YES.")
    ))
  }

  "when Column L is answered yes, column M is a mandatory field" in {
    val cellM = Cell("M", rowNumber, "")
    val cellL = Cell("L", rowNumber, "no")
    val row = Row(1,Seq(cellM,cellL))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellM,"mandatoryM","M01","'13. If shares were not listed on a recognised stock exchange, was valuation agreed with HMRC? (yes/no)' must be answered if '12. For lifting of restrictions, are the shares listed on a recognised stock exchange? (yes/no)' was answered with NO.")
    ))
  }

  "when Column L is answered yes, column N is a mandatory field" in {
    val cellN = Cell("N", rowNumber, "")
    val cellM = Cell("M", rowNumber, "yes")
    val row = Row(1,Seq(cellN,cellM))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellN,"mandatoryN","N01","'14. If yes, enter the HMRC reference given' must be answered if '13. If shares were not listed on a recognised stock exchange, was valuation agreed with HMRC? (yes/no)' was answered with YES.")
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

class OTHEROtherBenefitsV3ValidationTest extends PlaySpec with ERSValidationOTHEROtherBenefitsTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-other-benefits-validation-config"))

  "ERS Validation tests for OTHER Other Benefits" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

  "when Column B is answered yes, column C is a mandatory field" in {
    val cellC = Cell("C", rowNumber, "")
    val cellB = Cell("B", rowNumber, "yes")
    val row = Row(1,Seq(cellC,cellB))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellC,"mandatoryB","MB","'3. If yes enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' was answered with YES.")
    ))
  }
}

class OTHERConvertibleV3ValidationTest extends PlaySpec with ERSValidationOTHERConvertibleTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-convertible-validation-config"))

  "ERS Validation tests for OTHER Convertible" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
  }

  "when Column B is answered yes, column C is a mandatory field" in {
    val cellC = Cell("C", rowNumber, "")
    val cellB = Cell("B", rowNumber, "yes")
    val row = Row(1, Seq(cellC, cellB))
    val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
    resOpt mustBe Some(List(
      ValidationError(cellC, "mandatoryC", "C01", "'3. If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' was answered with YES.")
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

class OTHERNotionalV3ValidationTest extends PlaySpec with ERSValidationOTHERNotionalTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-notional-validation-config"))

  "ERS Validation tests for OTHER Notional" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)
    "when notional Column B is answered yes, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1, Seq(cellC, cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC, "mandatoryC", "C01", "'3.If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2.Is the event in relation to a disclosable tax avoidance scheme?(yes/no)' was answered with YES.")
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

class OTHEREnhancementV3ValidationTest extends PlaySpec with ERSValidationOTHEREnhancementTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-enhancement-validation-config"))

  "ERS Validation tests for OTHER Enhancement" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when enhancement Column B is answered yes, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1, Seq(cellC, cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC, "mandatoryC", "C01", "'3. If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme?(yes/no)' was answered with YES.")
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

class OTHERSoldV3ValidationTest extends PlaySpec with ERSValidationOTHERSoldTestData with ValidationTestRunner {

  val validator = DataValidator(ConfigFactory.load.getConfig("ers-other-sold-validation-config"))

  "ERS Validation tests for OTHER Sold" should {
    runTests(validator, getDescriptions, getTestData, getExpectedResults)

    "when sold Column B is answered yes, column C is a mandatory field" in {
      val cellC = Cell("C", rowNumber, "")
      val cellB = Cell("B", rowNumber, "yes")
      val row = Row(1, Seq(cellC, cellB))
      val resOpt: Option[List[ValidationError]] = validator.validateRow(row, Some(ValidationContext))
      resOpt mustBe Some(List(
        ValidationError(cellC, "mandatoryC", "C01", "'3. If yes, enter the eight-digit scheme reference number (SRN)' must be answered if '2. Is the event in relation to a disclosable tax avoidance scheme?(yes/no)' was answered with YES.")
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
