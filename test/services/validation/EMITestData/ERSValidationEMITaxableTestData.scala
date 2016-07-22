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

package services.validation.EMITestData

import uk.gov.hmrc.services.validation.Cell
import models.ValidationErrorData
import org.apache.commons.lang3.StringUtils

/**
 * Created by darryl on 26/01/16.
 */
trait ERSValidationEMITaxableTestData {

  val rowNumber:Int = 1

  def getDescriptions: List[String] = {
    val descriptions =
      List(
      "validate exerciseDate without returning ValidationErrors for valid data",
      "validate exerciseDate returning ValidationError for empty field",
      "validate exerciseDate returning ValidationError an invalid format",
      "validate disqualifyingEvent with no ValidationErrors for valid data",
      "validate disqualifyingEvent with ValidationErrors for invalid data",
      "validate natureOfDisqualifyingEvent with no ValidationErrors for valid data",
      "validate natureOfDisqualifyingEvent with ValidationErrors a number out of range",
      "validate natureOfDisqualifyingEvent with ValidationErrors for an alphanumeric value",
      "validate natureOfDisqualifyingEvent with ValidationErrors for a number larger than that allowed",
      "validate natureOfDisqualifyingEvent with ValidationErrors for a decimal number",
      "validate natureOfDisqualifyingEvent with ValidationErrors for negative number",
      "validate individualTaxExercise\\firstName without ValidationErrors for valid data",
      "validate individualTaxExercise\\firstName with ValidationErrors for a string too long",
      "validate individualTaxExercise\\firstName with ValidationErrors an empty field",
      "validate individualTaxExercise\\secondName without ValidationErrors for valid data",
      "validate individualTaxExercise\\secondName with ValidationErrors for a string too long",
      "validate individualTaxExercise\\surname without ValidationErrors for valid data",
      "validate individualTaxExercise\\surname with ValidationErrors for a string too long",
      "validate individualTaxExercise\\surname with ValidationErrors for an empty field",
      "validate individualTaxExercise\\nino with ValidationErrors for valid data",
      "validate individualTaxExercise\\nino with ValidationErrors for an invalid nino",
      "validate individualTaxExercise\\payeReference without ValidationErrors for valid data",
      "validate individualTaxExercise\\payeReference with ValidationErrors for invalid PAYE ref",
      "validate numberOfSharesAcquired without ValidationErrors for valid data",
      "validate numberOfSharesAcquired with ValidationErrors for a number with more than 2 digits following the decimal point",
      "validate numberOfSharesAcquired with ValidationErrors for an alphanumeric value",
      "validate numberOfSharesAcquired with ValidationErrors for a number too large",
      "validate actualMarketValueAtGrantDate without ValidationErrors for valid data",
      "validate actualMarketValueAtGrantDate with ValidationErrors for a number with over 4 decimal places",
      "validate actualMarketValueAtGrantDate with ValidationErrors for an alphanumeric value",
      "validate actualMarketValueAtGrantDate with ValidationErrors for a number too large",
      "validate exercisePricePaidToAcquireAShare without ValidationErrors for valid data",
      "validate exercisePricePaidToAcquireAShare with ValidationErrors for a number with over 4 decimal places",
      "validate exercisePricePaidToAcquireAShare with ValidationErrors for an alphanumeric value",
      "validate exercisePricePaidToAcquireAShare with ValidationErrors for a number too large",
      "validate actualMarketValuePerShareAtExerciseDate without ValidationErrors for valid data",
      "validate actualMarketValuePerShareAtExerciseDate with ValidationErrors for a number with over 4 decimal places",
      "validate actualMarketValuePerShareAtExerciseDate with ValidationErrors for an alphanumeric value",
      "validate actualMarketValuePerShareAtExerciseDate with ValidationErrors for a number too large",
      "validate unrestrictedMarketValuePerShareAtExerciseDate without ValidationErrors for valid data",
      "validate unrestrictedMarketValuePerShareAtExerciseDate with ValidationErrors for a number with over 4 decimal places",
      "validate unrestrictedMarketValuePerShareAtExerciseDate with ValidationErrors for an alphanumeric value",
      "validate unrestrictedMarketValuePerShareAtExerciseDate with ValidationErrors for a number too large",
      "validate totalAmountPaidToAcquireTheShares without ValidationErrors for valid data",
      "validate totalAmountPaidToAcquireTheShares with ValidationErrors for a number with over 4 decimal places",
      "validate totalAmountPaidToAcquireTheShares with ValidationErrors for an alphanumeric value",
      "validate totalAmountPaidToAcquireTheShares with ValidationErrors for a number too large",
      "validate sharesListedOnSE without ValidationErrors for valid data",
      "validate sharesListedOnSE with ValidationErrors for invalid data",
      "validate sharesListedOnSE with ValidationErrors for an empty field",
      "validate marketValueAgreedHMRC without ValidationErrors for valid data",
      "validate marketValueAgreedHMRC with ValidationErrors for invalid data",
      "validate hmrcRef without ValidationErrors for valid data",
      "validate hmrcRef with ValidationErrors for invalid data",
      "validate electionMadeUnderSection431 without ValidationErrors for valid data",
      "validate electionMadeUnderSection431 with ValidationErrors for invalid data",
      "validate electionMadeUnderSection431 with ValidationErrors for an empty field",
      "validate nicsElectionAgreementEnteredInto without ValidationErrors for valid data",
      "validate nicsElectionAgreementEnteredInto with ValidationErrors for invalid data",
      "validate nicsElectionAgreementEnteredInto with ValidationErrors for an empty field",
      "validate amountSubjectToPAYE without ValidationErrors for valid data",
      "validate amountSubjectToPAYE with ValidationErrors for a number with over 4 decimal places",
      "validate amountSubjectToPAYE with ValidationErrors for an alphanumeric value",
      "validate amountSubjectToPAYE with ValidationErrors for a number too large"
      )
    descriptions
  }

  def getTestData: List[Cell] = {
    val testData = List(
      Cell("A", rowNumber, "2015-06-04"),
      Cell("A", rowNumber, ""),
      Cell("A", rowNumber, "20150604"),
      Cell("B", rowNumber, "yes"),
      Cell("B", rowNumber, "abc"),
      Cell("C", rowNumber, "3"),
      Cell("C", rowNumber, "0"),
      Cell("C", rowNumber, "abc"),
      Cell("C", rowNumber, "88"),
      Cell("C", rowNumber, "7.8"),
      Cell("C", rowNumber, "-7"),
      Cell("D", rowNumber, "Billy"),
      Cell("D", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("D", rowNumber, ""),
      Cell("E", rowNumber, "Bob"),
      Cell("E", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("F", rowNumber, "Thornton"),
      Cell("F", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("F", rowNumber, ""),
      Cell("G", rowNumber, "AB123456C"),
      Cell("G", rowNumber, "abc"),
      Cell("H", rowNumber, "123/XZ55555555"),
      Cell("H", rowNumber, "abc???"),
      Cell("I", rowNumber, "100.00"),
      Cell("I", rowNumber, "100.04030"),
      Cell("I", rowNumber, "abc100"),
      Cell("I", rowNumber, StringUtils.leftPad("", 15, "1") + ".34"),
      Cell("J", rowNumber, "10.1234"),
      Cell("J", rowNumber, "10.1234567"),
      Cell("J", rowNumber, "123abc"),
      Cell("J", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234"),
      Cell("K", rowNumber, "10.1234"),
      Cell("K", rowNumber, "10.1234567"),
      Cell("K", rowNumber, "12nas"),
      Cell("K", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234"),
      Cell("L", rowNumber, "10.1234"),
      Cell("L", rowNumber, "10.1234567"),
      Cell("L", rowNumber, "12nas"),
      Cell("L", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234"),
      Cell("M", rowNumber, "10.1234"),
      Cell("M", rowNumber, "10.1234567"),
      Cell("M", rowNumber, "12nas"),
      Cell("M", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234"),
      Cell("N", rowNumber, "10.1234"),
      Cell("N", rowNumber, "10.1234567"),
      Cell("N", rowNumber, "12nas"),
      Cell("N", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234"),
      Cell("O", rowNumber, "yes"),
      Cell("O", rowNumber, "abc"),
      Cell("O", rowNumber, ""),
      Cell("P", rowNumber, "yes"),
      Cell("P", rowNumber, "abc"),
      Cell("Q", rowNumber, "aa12345678"),
      Cell("Q", rowNumber, "abc12345678901"),
      Cell("R", rowNumber, "yes"),
      Cell("R", rowNumber, "abc"),
      Cell("R", rowNumber, ""),
      Cell("S", rowNumber, "yes"),
      Cell("S", rowNumber, "abc"),
      Cell("S", rowNumber, ""),
      Cell("T", rowNumber, "10.1234"),
      Cell("T", rowNumber, "10.1234567"),
      Cell("T", rowNumber, "12nas"),
      Cell("T", rowNumber, StringUtils.leftPad("", 15, "1") + ".3234")
    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      None,
      Some(List(ValidationErrorData("MANDATORY","100","'1. Date option exercised (yyyy-mm-dd)' must have an entry."))),
      Some(List(ValidationErrorData("error.1","001","Enter a date that matches the yyyy-mm-dd pattern."))),
      None,
      Some(List(ValidationErrorData("error.2","002","Enter 'yes' or 'no'."))),
      None,
      Some(List(ValidationErrorData("error.3","003","Enter '1', '2', '3', '4', '5', '6', '7' or '8'."))),
      Some(List(
        ValidationErrorData("error.3","003","Enter '1', '2', '3', '4', '5', '6', '7' or '8'."),
        ValidationErrorData("error.4","004","This entry must be a number made up of digits."),
        ValidationErrorData("error.5","005","This entry is larger than the maximum number value allowed."),
        ValidationErrorData("error.6","006","This entry must be a whole number."),
        ValidationErrorData("error.7","007","This entry must be either a positive number or a zero.")
      )),
      Some(List(
        ValidationErrorData("error.3","003","Enter '1', '2', '3', '4', '5', '6', '7' or '8'."),
        ValidationErrorData("error.5","005","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.6","006","This entry must be a whole number."))),
      Some(List(
        ValidationErrorData("error.3","003","Enter '1', '2', '3', '4', '5', '6', '7' or '8'."),
        ValidationErrorData("error.4","004","This entry must be a number made up of digits."),
        ValidationErrorData("error.5","005","This entry is larger than the maximum number value allowed."),
        ValidationErrorData("error.6","006","This entry must be a whole number."),
        ValidationErrorData("error.7","007","This entry must be either a positive number or a zero.")
      )),
      None,
      Some(List(ValidationErrorData("error.8","008","Enter a first name (must be less than 36 characters)."))),
      Some(List(ValidationErrorData("MANDATORY","100","'4. Employee first name' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.9","009","Must be less than 36 characters."))),
      None,
      Some(List(ValidationErrorData("error.10","010","Enter a last name (must be less than 36 characters)."))),
      Some(List(ValidationErrorData("MANDATORY","100","'6. Employee last name' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.11","011","The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter."))),
      None,
      Some(List(ValidationErrorData("error.12","012","PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      None,
      Some(List(ValidationErrorData("error.13","013","Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.13","013","Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."),
        ValidationErrorData("error.14","014","This entry must be a number made up of digits."),
        ValidationErrorData("error.15","015","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.15","015","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.16","016","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.16","016","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.17","017","This entry must be a number made up of digits."),
        ValidationErrorData("error.18","018","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.18","018","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.21","021","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.21","021","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.22","022","This entry must be a number made up of digits."),
        ValidationErrorData("error.23","023","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.23","023","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.24","024","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.24","024","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.25","025","This entry must be a number made up of digits."),
        ValidationErrorData("error.26","026","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.26","026","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.27","027","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.27","027","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.28","028","This entry must be a number made up of digits."),
        ValidationErrorData("error.29","029","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.29","029","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.30","030","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.30","030","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.31","031","This entry must be a number made up of digits."),
        ValidationErrorData("error.32","032","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.32","032","This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.33","033","Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'15. Is the company listed on a recognised stock exchange? (yes/no)If yes go to question 18If no go to question 16' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.34","034","Enter 'yes' or 'no'."))),
      None,
      Some(List(ValidationErrorData("error.35","035","Enter the HMRC reference (must be less than 11 characters)."))),
      None,
      Some(List(ValidationErrorData("error.36","036","Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'18. Has an election under Section 431(1) been made to disregard restrictions? (yes/ no)' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.37","037","Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'19. Has a National Insurance Contribution election or agreement been operated (yes/no)' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.38","038","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.38","038","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.39","039","This entry must be a number made up of digits."),
        ValidationErrorData("error.40","040","This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.40","040","This entry is larger than the maximum number value allowed.")))
    )
    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2015-06-04"),
      Cell("B", rowNumber, "yes"),
      Cell("C", rowNumber, "3"),
      Cell("D", rowNumber, "Billy"),
      Cell("E", rowNumber, "Bob"),
      Cell("F", rowNumber, "Thornton"),
      Cell("G", rowNumber, "AB123456C"),
      Cell("H", rowNumber, "123/XZ55555555"),
      Cell("I", rowNumber, "100.00"),
      Cell("J", rowNumber, "10.1234"),
      Cell("K", rowNumber, "10.1234"),
      Cell("L", rowNumber, "10.1234"),
      Cell("M", rowNumber, "10.1234"),
      Cell("N", rowNumber, "10.1234"),
      Cell("O", rowNumber, "yes"),
      Cell("P", rowNumber, "yes"),
      Cell("Q", rowNumber, "aa12345678"),
      Cell("R", rowNumber, "yes"),
      Cell("S", rowNumber, "yes"),
      Cell("T", rowNumber, "10.1234")
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, ""),
      Cell("B", rowNumber, "abc"),
      Cell("C", rowNumber, "0"),
      Cell("D", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("E", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("F", rowNumber, StringUtils.leftPad("", 45, "A")),
      Cell("G", rowNumber, "abc"),
      Cell("H", rowNumber, "abc???"),
      Cell("I", rowNumber, "21.21222"),
      Cell("J", rowNumber, "21.21222"),
      Cell("K", rowNumber, "21.21222"),
      Cell("L", rowNumber, "21.21222"),
      Cell("M", rowNumber, "21.21222"),
      Cell("N", rowNumber, "21.21222"),
      Cell("O", rowNumber, "abc"),
      Cell("P", rowNumber, "abc"),
      Cell("Q", rowNumber, "abc12345678901"),
      Cell("R", rowNumber, ""),
      Cell("S", rowNumber, ""),
      Cell("T", rowNumber, "21.2122212")
    )
    rowData
  }

}
