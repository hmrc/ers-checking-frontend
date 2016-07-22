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

package services.validation.OTHERTestData

import uk.gov.hmrc.services.validation.Cell
import models.ValidationErrorData
import org.apache.commons.lang3.StringUtils

trait ERSValidationOTHERAcquisitionTestData {

  val rowNumber:Int = 1

  def getDescriptions: List[String] = {
    val descriptions =
      List(
        //A
        "validate dateOfEvent without ValidationErrors for valid data",
        "validate dateOfEvent with ValidationErrors for invalid data",
        "validate dateOfEvent with ValidationErrors for an empty field",
        //B
        "validate inRelationToASchemeWithADOTASRef without ValidationErrors for valid data",
        "validate inRelationToASchemeWithADOTASRef with ValidationErrors for a invalid data",
        "validate inRelationToASchemeWithADOTASRef with ValidationErrors for an empty field",
        //C
        "validate dotasRef without ValidationErrors for a valid data",
        "validate dotasRef with ValidationErrors for a invalid data",
        //D,
        "validate individualOptions\\firstName without ValidationErrors for valid data",
        "validate individualOptions\\firstName with ValidationErrors for a string too long",
        "validate individualOptions\\firstName with ValidationErrors for an empty field",
        //E
        "validate individualOptions\\secondName without ValidationErrors for valid data",
        "validate individualOptions\\secondName without ValidationErrors for a string too long",
        //F
        "validate individualOptions\\surname without ValidationErrors for valid data",
        "validate individualOptions\\surname with ValidationErrors for a string too long",
        "validate individualOptions\\surname with ValidationErrors for an empty field",
        //G
        "validate individualOptions\\nino without ValidationErrors for valid data",
        "validate individualOptions\\nino with ValidationErrors for invalid data",
        //H
        "validate individualOptions\\payeReference without ValidationErrors for valid data",
        "validate individualOptions\\payeReference with ValidationErrors for invalid data",
        //I
        "validate secAwdCompany\\compnayName without ValidationErrors for valid data",
        "validate secAwdCompany\\compnayName without ValidationErrors for a string too long",
        "validate secAwdCompany\\compnayName with ValidationErrors for an empty field",
        //J
        "validate secAwdCompany\\companyAddress\\addressLine1 without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\addressLine1 with ValidationErrors for a string too long",
        "validate secAwdCompany\\companyAddress\\addressLine1 with ValidationErrors for an empty field",
        //K
        "validate secAwdCompany\\companyAddress\\addressLine2 without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\addressLine2 with ValidationErrors for a string too long",
        //L
        "validate secAwdCompany\\companyAddress\\addressLine3 without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\addressLine3 with ValidationErrors for a string too long",
        //M
        "validate secAwdCompany\\companyAddress\\addressLine4 without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\addressLine4 with ValidationErrors for a string too long",
        //N
        "validate secAwdCompany\\companyAddress\\country without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\country with ValidationErrors for a string too long",
        //O
        "validate secAwdCompany\\companyAddress\\postCode without ValidationErrors for valid data",
        "validate secAwdCompany\\companyAddress\\postCode with ValidationErrors for a string too long",
        //P
        "validate secAwdCompany\\companyCRN without ValidationErrors for valid data",
        "validate secAwdCompany\\companyCRN with ValidationErrors for invalid data",
        //Q
        "validate secAwdCompany\\companyCTRef without ValidationErrors for valid data",
        "validate secAwdCompany\\companyCTRef with ValidationErrors for invalid data",
        //R
        "validate secAwdCompany\\companyPAYERef without ValidationErrors for valid data",
        "validate secAwdCompany\\companyPAYERef with ValidationErrors for invalid data",
        //S
        "validate secAwdDescription without ValidationErrors for valid data",
        "validate secAwdDescription with ValidationErrors for out of the specified range",
        "validate secAwdDescription with ValidationErrors for an alphanumeric string",
        "validate secAwdDescription with ValidationErrors for a number larger than that allowed",
        "validate secAwdDescription with ValidationErrors for a number with decimal places",
        "validate secAwdDescription with ValidationErrors for a negative number",
        "validate secAwdDescription with ValidationErrors for an empty field",
        //T
        "validate sharesPartOfLargestClass without ValidationErrors for valid data",
        "validate sharesPartOfLargestClass with ValidationErrors for invalid data",
        "validate sharesPartOfLargestClass with ValidationErrors for an empty field",
        //U
        "validate sharesListedOnSE without ValidationErrors for valid data",
        "validate sharesListedOnSE with ValidationErrors for invalid data",
        //V
        "validate marketValueAgreedHMRC without ValidationErrors for valid data",
        "validate marketValueAgreedHMRC with ValidationErrors for invalid data",
        //W
        "validate hmrcRef without ValidationErrors for valid data",
        "validate hmrcRef with ValidationErrors for invalid data",
        //X
        "validate numberOfSharesIssued without ValidationErrors for valid data",
        "validate numberOfSharesIssued with ValidationErrors for invalid data",
        //Y
        "validate restrictedUnrestrictedConvertible without ValidationErrors for valid data",
        "validate restrictedUnrestrictedConvertible with ValidationErrors for invalid data",
        "validate restrictedUnrestrictedConvertible with ValidationErrors for an empty field",
        //Z
        "validate natureOfRestriction without ValidationErrors for valid data",
        "validate natureOfRestriction with ValidationErrors for invalid data",
        //AA
        "validate lengthOfTimeOfRestrictionsInYears without ValidationErrors for valid data",
        "validate lengthOfTimeOfRestrictionsInYears with ValidationErrors for invalid data",
        //AB
        "validate actualMarketValuePerShareAtAcquisitionDate without ValidationErrors for valid data",
        "validate actualMarketValuePerShareAtAcquisitionDate with ValidationErrors for invalid data",
        //AC
        "validate unrestrictedMarketValuePerShareAtAcquisitionDate without ValidationErrors for valid data",
        "validate unrestrictedMarketValuePerShareAtAcquisitionDate with ValidationErrors for invalid data",
        //AD
        "validate hasAnElectionBeenMadeToDisregardRestrictions without ValidationErrors for valid data",
        "validate hasAnElectionBeenMadeToDisregardRestrictions with ValidationErrors for invalid data",
        //AE
        "validate allSomeRestrictionsDisregarded without ValidationErrors for valid data",
        "validate allSomeRestrictionsDisregarded with ValidationErrors for invalid data",
        //AF
        "validate marketValuePerShareIgnoringConversionRights without ValidationErrors for valid data",
        "validate marketValuePerShareIgnoringConversionRights with ValidationErrors for invalid data",
        //AG
        "validate totalPricePaid without ValidationErrors for valid data",
        "validate totalPricePaid with ValidationErrors for invalid data",
        //AH
        "validate paidInSterling without ValidationErrors for valid data",
        "validate paidInSterling with ValidationErrors for invalid data",
        "validate paidInSterling with ValidationErrors for an empty field",
        //AI
        "validate natureOfArtificialReductionByReason without ValidationErrors for valid data",
        "validate natureOfArtificialReductionByReason with ValidationErrors for invalid data",
        "validate natureOfArtificialReductionByReason with ValidationErrors for an empty field",
        //AJ
        "validate marketValuePerShareIgnoringConversionRights-2 without ValidationErrors for valid data",
        "validate marketValuePerShareIgnoringConversionRights-2 with ValidationErrors for invalid data",
        //AK
        "validate sharesIssuedUnderAnEmployeeShareholderArrangement without ValidationErrors for valid data",
        "validate sharesIssuedUnderAnEmployeeShareholderArrangement with ValidationErrors for invalid data",
        //AL
        "validate totalMarketValueOfShares2000OrMore without ValidationErrors for valid data",
        "validate totalMarketValueOfShares2000OrMore with ValidationErrors for invalid data"
      )
    descriptions
  }

  def getTestData: List[Cell] = {
    val testData = List(
      Cell("A",rowNumber,"2014-08-30"),
      Cell("A",rowNumber,"2014-08/30"),
      Cell("A",rowNumber,""),
      Cell("B",rowNumber,"yes"),
      Cell("B",rowNumber,"yav"),
      Cell("B",rowNumber,""),
      Cell("C",rowNumber,"12345678"),
      Cell("C",rowNumber,"12348"),
      Cell("D",rowNumber,"Billy"),
      Cell("D",rowNumber,StringUtils.leftPad("",45, "A")),
      Cell("D",rowNumber,""),
      Cell("E",rowNumber,"Bob"),
      Cell("E",rowNumber,StringUtils.leftPad("",45, "A")),
      Cell("F",rowNumber,"Thornton"),
      Cell("F",rowNumber,StringUtils.leftPad("",45, "A")),
      Cell("F",rowNumber,""),
      Cell("G",rowNumber,"AB123456A"),
      Cell("G",rowNumber,"AB1234A"),
      Cell("H",rowNumber,"123/XZ55555555"),
      Cell("H",rowNumber,"123XZ55555555???"),
      Cell("I",rowNumber,"Company"),
      Cell("I",rowNumber,StringUtils.leftPad("",150, "A")),
      Cell("I",rowNumber,""),
      Cell("J",rowNumber,"1 Beth Street"),
      Cell("J",rowNumber,StringUtils.leftPad("",30, "A")),
      Cell("J",rowNumber,""),
      Cell("K",rowNumber,"Bucknall"),
      Cell("K",rowNumber,StringUtils.leftPad("",30, "A")),
      Cell("L",rowNumber,"Stoke"),
      Cell("L",rowNumber,StringUtils.leftPad("",30, "A")),
      Cell("M",rowNumber,"Staffordshire"),
      Cell("M",rowNumber,StringUtils.leftPad("",20, "A")),
      Cell("N",rowNumber,"United Kingdom"),
      Cell("N",rowNumber,StringUtils.leftPad("",20, "A")),
      Cell("O",rowNumber,"SE1 2AB"),
      Cell("O",rowNumber,"S2AB1"),
      Cell("P",rowNumber,"AC097609"),
      Cell("P",rowNumber,"98097!6AB"),
      Cell("Q",rowNumber,"1234567890"),
      Cell("Q",rowNumber,"123456789012"),
      Cell("R",rowNumber,"123/XZ55555555"),
      Cell("R",rowNumber,"123XZ55555555???"),
      Cell("S",rowNumber,"3"),
      Cell("S",rowNumber,"0"),
      Cell("S",rowNumber,"anc"),
      Cell("S",rowNumber,"11"),
      Cell("S",rowNumber,"7.1"),
      Cell("S",rowNumber,"-7"),
      Cell("S",rowNumber,""),
      Cell("T",rowNumber,"yes"),
      Cell("T",rowNumber,"ys"),
      Cell("T",rowNumber,""),
      Cell("U",rowNumber,"no"),
      Cell("U",rowNumber,"n"),
      Cell("V",rowNumber,"no"),
      Cell("V",rowNumber,"n"),
      Cell("W",rowNumber,"12345678"),
      Cell("W",rowNumber,"12345678910"),
      Cell("X",rowNumber,"120.55"),
      Cell("X",rowNumber,"123456789111.123"),
      Cell("Y",rowNumber,"2"),
      Cell("Y",rowNumber,"4"),
      Cell("Y",rowNumber,""),
      Cell("Z",rowNumber,"3"),
      Cell("Z",rowNumber,"4"),
      Cell("AA",rowNumber,"123456.2"),
      Cell("AA",rowNumber,"1234567.123"),
      Cell("AB",rowNumber,"1234567891123.1234"),
      Cell("AB",rowNumber,"12345678911235.12345"),
      Cell("AC",rowNumber,"1234567891123.1234"),
      Cell("AC",rowNumber,"12345678911235.12345"),
      Cell("AD",rowNumber,"yes"),
      Cell("AD",rowNumber,"gus"),
      Cell("AE",rowNumber,"all"),
      Cell("AE",rowNumber,"same"),
      Cell("AF",rowNumber,"1234567891123.1234"),
      Cell("AF",rowNumber,"12345678911236.12345"),
      Cell("AG",rowNumber,"1234567891123.1234"),
      Cell("AG",rowNumber,"12345678911236.12345"),
      Cell("AH",rowNumber,"yes"),
      Cell("AH",rowNumber,"gus"),
      Cell("AH",rowNumber,""),
      Cell("AI",rowNumber,"yes"),
      Cell("AI",rowNumber,"gus"),
      Cell("AI",rowNumber,""),
      Cell("AJ",rowNumber,"2"),
      Cell("AJ",rowNumber,"4"),
      Cell("AK",rowNumber,"yes"),
      Cell("AK",rowNumber,"gus"),
      Cell("AL",rowNumber,"yes"),
      Cell("AL",rowNumber,"gus"),
      Cell("AM",rowNumber,"yes"),
      Cell("AM",rowNumber,"gus"),
      Cell("AM",rowNumber,""),
      Cell("AN",rowNumber,"yes"),
      Cell("AN",rowNumber,"gus"),
      Cell("AN",rowNumber,"")
    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      //A
      None,
      Some(List(ValidationErrorData("error.1","001","The date must match the yyyy-mm-dd pattern."))),
      Some(List(ValidationErrorData("MANDATORY","100","'1. Date of event (yyyy-mm-dd)' must have an entry."))),
      //B
      None,
      Some(List(ValidationErrorData("error.2","002","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'2. Is the event in relation to a disclosable tax avoidance scheme? (yes/no)' must have an entry."))),
      //C
      None,
      Some(List(ValidationErrorData("error.3","003","The scheme reference number is missing."))),
      //D
      None,
      Some(List(ValidationErrorData("error.4","004","This entry must contain 35 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY","100","'4. Employee first name' must have an entry."))),
      //E
      None,
      Some(List(ValidationErrorData("error.5","005","This entry must contain 35 characters or less."))),
      //F
      None,
      Some(List(ValidationErrorData("error.6","006","This entry must contain 35 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY","100","'6. Employee last name' must have an entry."))),
      //G
      None,
      Some(List(ValidationErrorData("error.7","007","The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter."))),
      //H
      None,
      Some(List(ValidationErrorData("error.8","008","PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      //I
      None,
      Some(List(ValidationErrorData("error.9","009","This entry must contain 120 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY","100","'9. Name of the company whose securities acquired' must have an entry."))),
      //J
      None,
      Some(List(ValidationErrorData("error.10","010","This entry must contain 27 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY","100","'10. Company whose securities acquired – Address line 1' must have an entry."))),
      //K
      None,
      Some(List(ValidationErrorData("error.11","011","This entry must contain 27 characters or less."))),
      //L
      None,
      Some(List(ValidationErrorData("error.12","012","This entry must contain 27 characters or less."))),
      //M
      None,
      Some(List(ValidationErrorData("error.13","013","This entry must contain 18 characters or less."))),
      //N
      None,
      Some(List(ValidationErrorData("error.14","014","This entry must contain 18 characters or less."))),
      //O
      None,
      Some(List(ValidationErrorData("error.15","015","Must be less than 9 characters and only have capital letters."))),
      //P
      None,
      Some(List(ValidationErrorData("error.16","016","The Company Reference Number must contain 10 characters or less."))),
      //Q
      None,
      Some(List(ValidationErrorData("error.17","017","The Corporation Tax reference must be a 10 digit number."))),
      //R
      None,
      Some(List(ValidationErrorData("error.18","018","PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      //S
      None,
      Some(List(
        ValidationErrorData("error.19","019","This entry must be within the specified number range."),
        ValidationErrorData("error.21","021","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(
        ValidationErrorData("error.19","019","This entry must be within the specified number range."),
        ValidationErrorData("error.20","020","This entry must be a number made up of digits."),
        ValidationErrorData("error.21","021","This entry is larger than the maximum number value allowed."),
        ValidationErrorData("error.22","022","This entry must be a whole number."),
        ValidationErrorData("error.23","023","This entry must be either a positive number or a zero.")
      )),
      Some(List(
        ValidationErrorData("error.19","019","This entry must be within the specified number range."),
        ValidationErrorData("error.21","021","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(
        ValidationErrorData("error.22","022","This entry must be a whole number."),
        ValidationErrorData("error.23","023","This entry must be either a positive number or a zero.")
      )),
      Some(List(
        ValidationErrorData("error.19","019","This entry must be within the specified number range."),
        ValidationErrorData("error.20","020","This entry must be a number made up of digits."),
        ValidationErrorData("error.23","023","This entry must be either a positive number or a zero.")
      )),
      Some(List(ValidationErrorData("MANDATORY","100","'19. Description of security. Enter a number from 1 to 9. Follow the link in cell A7 for a list of security types' must have an entry."))),
      //T
      None,
      Some(List(ValidationErrorData("error.24","024","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'20. If the securities are not shares enter ' no' and go to question 24 If the securities are shares, are they part of the largest class of shares in the company? (yes/no)' must have an entry."))),
      //U
      None,
      Some(List(ValidationErrorData("error.25","025","This entry must be 'yes' or 'no'."))),
      //V
      None,
      Some(List(ValidationErrorData("error.26","026","This entry must be 'yes' or 'no'."))),
      //W
      None,
      Some(List(ValidationErrorData("error.27","027","The HMRC reference must contain 10 characters or less (letters, numbers or both)."))),
      //X
      None,
      Some(List(ValidationErrorData("error.28","028","This entry must be a number with 2 digits after the decimal point."))),
      //Y
      None,
      Some(List(ValidationErrorData("error.29","029","This entry must be within the specified number range."))),
      Some(List(ValidationErrorData("MANDATORY","100","'25. Security type. Enter a number from 1 to 3, (follow the link at cell A7 for a list of security types). If restricted go to next question. If convertible go to question 32. If both restricted and convertible enter 1 and answer all questions 26 to 32. If neither restricted nor convertible go to question 29.' must have an entry."))),
      //Z
      None,
      Some(List(ValidationErrorData("error.30","030","This entry must be within the specified number range."))),
      //AA
      None,
      Some(List(ValidationErrorData("error.31","031","This entry must be a number with 1 digits after the decimal point."))),
      //AB
      None,
      Some(List(ValidationErrorData("error.32","032","This entry must be a number with 4 digits after the decimal point."))),
      //AC
      None,
      Some(List(ValidationErrorData("error.33","033","This entry must be a number with 4 digits after the decimal point."))),
      //AD
      None,
      Some(List(ValidationErrorData("error.34","034","This entry must be 'yes' or 'no'."))),
      //AE
      None,
      Some(List(ValidationErrorData("error.35","035","This entry must be 'all' or 'some'."))),
      //AF
      None,
      Some(List(ValidationErrorData("error.36","036","This entry must be a number with 4 digits after the decimal point."))),
      //AG
      None,
      Some(List(ValidationErrorData("error.37","037","This entry must be a number with 4 digits after the decimal point."))),
      //AH
      None,
      Some(List(ValidationErrorData("error.38","038","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'34. Was the price paid in pounds sterling? (yes/no)' must have an entry."))),
      //AI
      None,
      Some(List(ValidationErrorData("error.39","039","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'35. Was there an artificial reduction in value on acquisition? (yes/no) If 'yes' go to question 36, if 'No' go to question 37' must have an entry."))),
      //AJ
      None,
      Some(List(ValidationErrorData("error.40","040","This entry must be within the specified number range."))),
      //AK
      None,
      Some(List(ValidationErrorData("error.41","041","This entry must be 'yes' or 'no'."))),
      //AL
      None,
      Some(List(ValidationErrorData("error.42","042","This entry must be 'yes' or 'no'."))),
      //AM
      None,
      Some(List(ValidationErrorData("error.43","043","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'39. Was PAYE operated? (yes/no)"))),
      //AN
      None,
      Some(List(ValidationErrorData("error.44","044","This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'40. Was any adjustment made for amounts subject to apportionment for residence or duties outside the UK (yes/no)' must have an entry.")))
    )
    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A",rowNumber,"2014-08-30"),
      Cell("B",rowNumber,"yes")
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A",rowNumber,"2014-08/30")
    )
    rowData
  }

}
