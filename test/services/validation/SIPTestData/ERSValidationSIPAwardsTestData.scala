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

package services.validation.SIPTestData

import uk.gov.hmrc.services.validation.Cell
import models.ValidationErrorData

trait ERSValidationSIPAwardsTestData {
  val rowNumber:Int = 1

  def getDescriptions: List[String] = {
    val descriptions =
      List(
        //column A
        "When dateOfEvent conforms to the expected date format, no validation error should be raised",
        "Return an error message when dateOfEvent does not conform to the expect date format",
        "Return an error message when dateOfEvent has been left empty",
        //column B
        "When numberOfIndividualsAwardedShares conforms to the expected number format, no validation error should be raised",
        "Return an error message when numberOfIndividualsAwardedShares exceeds the maximum value allowed",
        "Return an error message when numberOfIndividualsAwardedShares is not a whole number",
        //column C
        "When awarded/typeOfAward conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/typeOfAward is not within the specified range",
        "Return an error message when awarded/typeOfAward exceeds the maximum allowed value",
        "Return an error message when awarded/typeOfAward is not a whole number",
        "Return an error message when awarded/typeOfAward is not a positive number",
        "Return an error message when awarded/typeOfAward has been left empty",
        //column D
        "When awarded/freePerformanceConditions is either yes or no, no validation error should be raised",
        "Return an error message when awarded/freePerformanceConditions is not a yes or no answer",
        //column E
        "When awarded/matchingRatio is a valid ratio, no validation error should be raised",
        "This entry must match the specified ratio.",
        //column F
        "When awarded/marketValuePerShareOnAcquisitionOrAward conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/marketValuePerShareOnAcquisitionOrAward does not have 4 digits after the decimal point",
        "Return an error message when awarded/marketValuePerShareOnAcquisitionOrAward is not a number",
        "Return an error message when awarded/marketValuePerShareOnAcquisitionOrAward is larger than the maximum value allowed",
        //column G
        "When awarded/totalNumberOfSharesAwarded conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalNumberOfSharesAwarded does not have 2 digits after the decimal point",
        "Return an error message when awarded/totalNumberOfSharesAwarded is not a number",
        "Return an error message when awarded/totalNumberOfSharesAwarded is larger than the maximum value allowed",
        //column H
        "When awarded/totalValueOfSharesAwarded conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalValueOfSharesAwarded does not have 2 digits after the decimal point",
        "Return an error message when awarded/totalValueOfSharesAwarded is not a number",
        "Return an error message when awarded/totalValueOfSharesAwarded is larger than the maximum value allowed",
        //column I
        "When awarded/totalMatchingAwardsPerEmployeeGreaterThan3600 conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeGreaterThan3600 is larger than the maximum value allowed",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeGreaterThan3600 is not a whole number",
        //column J
        "When awarded/totalFreeAwardsPerEmployeeAtLimitOf3000 conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalFreeAwardsPerEmployeeAtLimitOf3000 is larger than the maximum value allowed",
        "Return an error message when awarded/totalFreeAwardsPerEmployeeAtLimitOf3000 is not a whole number",
        //column K
        "When awarded/totalPartnershipAwardsPerEmployeeGreaterThan1800 conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalPartnershipAwardsPerEmployeeGreaterThan1800 is larger than the maximum value allowed",
        "Return an error message when awarded/totalPartnershipAwardsPerEmployeeGreaterThan1800 is not a whole number",
        //column L
        "When awarded/totalPartnershipAwardsPerEmployeeAtLimitOf1500 conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalPartnershipAwardsPerEmployeeAtLimitOf1500 is larger than the maximum value allowed",
        "Return an error message when awarded/totalPartnershipAwardsPerEmployeeAtLimitOf1500 is not a whole number",
        //column M
        "When awarded/totalMatchingAwardsPerEmployeeGreaterThan3600B conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeGreaterThan3600B is larger than the maximum value allowed",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeGreaterThan3600B is not a whole number",
        //column N
        "When awarded/totalMatchingAwardsPerEmployeeAtLimitOf3000 conforms to the expected number format, no validation error should be raised",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeAtLimitOf3000 is larger than the maximum value allowed",
        "Return an error message when awarded/totalMatchingAwardsPerEmployeeAtLimitOf3000 is not a whole number",
        //column O
        "When awarded/sharesListedOnSE is yes or no, no validation error should be raised",
        "Return an error message when awarded/sharesListedOnSE is not yes or no",
        "Return an error message when awarded/sharesListedOnSE is left empty",
        //column P
        "When awarded/marketValueAgreedHMRC is yes or no, no validation error should be raised",
        "Return an error message when awarded/marketValueAgreedHMRC is not yes or no",
        //column Q
        "When awarded/hmrcRef conforms to the expected HMRC Ref format, no validation error should be raised",
        "Return an error message when awarded/hmrcRef is not a valid HMRC Ref"
      )
    descriptions
  }

  def getTestData: List[Cell] = {
    val testData = List(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("A", rowNumber, "12-10-2014"),
      Cell("A", rowNumber, ""),
      Cell("B", rowNumber, "1234"),
      Cell("B", rowNumber, "1234567"),
      Cell("B", rowNumber, "abc123"),
      Cell("C", rowNumber, "3"),
      Cell("C", rowNumber, "6"),
      Cell("C", rowNumber, "10"),
      Cell("C", rowNumber, "2.5"),
      Cell("C", rowNumber, "-2"),
      Cell("C", rowNumber, ""),
      Cell("D", rowNumber, "Yes"),
      Cell("D", rowNumber, "yess"),
      Cell("E", rowNumber, "2:1"),
      Cell("E", rowNumber, "2.5/1"),
      Cell("F", rowNumber, "1234567890123.1234"),
      Cell("F", rowNumber, "1234.12345"),
      Cell("F", rowNumber, "oneTwoThree"),
      Cell("F", rowNumber, "123456789012345.1234"),
      Cell("G", rowNumber, "12345678901.12"),
      Cell("G", rowNumber, "1234.12345"),
      Cell("G", rowNumber, "oneTwoThree"),
      Cell("G", rowNumber, "123456789012345.12"),
      Cell("H", rowNumber, "1234567890123.1234"),
      Cell("H", rowNumber, "1234.12345"),
      Cell("H", rowNumber, "oneTwoThree"),
      Cell("H", rowNumber, "123456789012345.1234"),
      Cell("I", rowNumber, "123456"),
      Cell("I", rowNumber, "1234567"),
      Cell("I", rowNumber, "12.5"),
      Cell("J", rowNumber, "123456"),
      Cell("J", rowNumber, "1234567"),
      Cell("J", rowNumber, "12.5"),
      Cell("K", rowNumber, "123456"),
      Cell("K", rowNumber, "1234567"),
      Cell("K", rowNumber, "12.5"),
      Cell("L", rowNumber, "123456"),
      Cell("L", rowNumber, "1234567"),
      Cell("L", rowNumber, "12.5"),
      Cell("M", rowNumber, "123456"),
      Cell("M", rowNumber, "1234567"),
      Cell("M", rowNumber, "12.5"),
      Cell("N", rowNumber, "123456"),
      Cell("N", rowNumber, "1234567"),
      Cell("N", rowNumber, "12.5"),
      Cell("O", rowNumber, "Yes"),
      Cell("O", rowNumber, "yess"),
      Cell("O", rowNumber, ""),
      Cell("P", rowNumber, "Yes"),
      Cell("P", rowNumber, "yess"),
      Cell("Q", rowNumber, "aa12345678"),
      Cell("Q", rowNumber, "abcada123456782145")


    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      //column A
      None,
      Some(List(ValidationErrorData("error.1", "001", "Enter a date that matches the yyyy-mm-dd pattern."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'1. Date of event(yyyy-mm-dd)' must have an entry."))),
      //column B
      None,
      Some(List(ValidationErrorData("error.2", "002", "Must be a whole number and be less than 1,000,000."))),
      Some(List(
        ValidationErrorData("error.2", "002", "Must be a whole number and be less than 1,000,000."),
        ValidationErrorData("error.3", "003", "This entry must be a whole number."))
      ),
      //column C
      None,
      Some(List(ValidationErrorData("error.4", "004", "Enter '1', '2', '3' or '4'."))),
      Some(List(
        ValidationErrorData("error.4", "004", "Enter '1', '2', '3' or '4'."),
        ValidationErrorData("error.5", "005", "This entry is larger than the maximum number value allowed."))),
      Some(List(
        ValidationErrorData("error.4", "004", "Enter '1', '2', '3' or '4'."),
        ValidationErrorData("error.5", "005", "This entry is larger than the maximum number value allowed."),
        ValidationErrorData("error.6", "006", "This entry must be a whole number."))),
      Some(List(
        ValidationErrorData("error.4", "004", "Enter '1', '2', '3' or '4'."),
        ValidationErrorData("error.5", "005", "This entry is larger than the maximum number value allowed."),
        ValidationErrorData("error.7", "007", "This entry must be either a positive number or a zero."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'3. Type of shares awarded Enter a number from 1 to 4 depending on the type of share awarded. Follow the link at cell B10 for a list of the types of share which can be awarded' must have an entry."))),
      //column D
      None,
      Some(List(ValidationErrorData("error.8", "008", "Enter 'yes' or 'no'."))),
      //column E
      None,
      Some(List(ValidationErrorData("error.9", "009", "Enter the ratio of the matching shares (numbers must be separated by a ':' or '/', for example, 2:1 or 2/1)."))),
      //column F
      None,
      Some(List(ValidationErrorData("error.10", "010", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.10", "010", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.11", "011", "This entry must be a number made up of digits."),
        ValidationErrorData("error.12", "012", "This entry is larger than the maximum number value allowed."))
      ),
      Some(List(ValidationErrorData("error.12", "012", "This entry is larger than the maximum number value allowed."))),
      //column G
      None,
      Some(List(ValidationErrorData("error.13", "013", "Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.13", "013", "Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."),
        ValidationErrorData("error.14", "014", "This entry must be a number made up of digits."),
        ValidationErrorData("error.15", "015", "This entry is larger than the maximum number value allowed."))
      ),
      Some(List(ValidationErrorData("error.15", "015", "This entry is larger than the maximum number value allowed."))),
      //column H
      None,
      Some(List(ValidationErrorData("error.16", "016", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.16", "016", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.17", "017", "This entry must be a number made up of digits."),
        ValidationErrorData("error.18", "018", "This entry is larger than the maximum number value allowed."))
      ),
      Some(List(ValidationErrorData("error.18", "018", "This entry is larger than the maximum number value allowed."))),
      //column I
      None,
      Some(List(ValidationErrorData("error.19", "019", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.20", "020", "This entry must be a whole number."))),
      //column J
      None,
      Some(List(ValidationErrorData("error.21", "021", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.22", "022", "This entry must be a whole number."))),
      //column K
      None,
      Some(List(ValidationErrorData("error.23", "023", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.24", "024", "This entry must be a whole number."))),
      //column L
      None,
      Some(List(ValidationErrorData("error.25", "025", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.26", "026", "This entry must be a whole number."))),
      //column M
      None,
      Some(List(ValidationErrorData("error.27", "027", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.28", "028", "This entry must be a whole number."))),
      //column N
      None,
      Some(List(ValidationErrorData("error.29", "029", "Must be a whole number and be less than 1,000,000."))),
      Some(List(ValidationErrorData("error.30", "030", "This entry must be a whole number."))),
      //column O
      None,
      Some(List(ValidationErrorData("error.31", "031", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'15. Are the shares listed on a recognised stock exchange? (yes/no)' must have an entry."))),
      //column P
      None,
      Some(List(ValidationErrorData("error.32", "032", "Enter 'yes' or 'no'."))),
      //column Q
      None,
      Some(List(ValidationErrorData("error.33", "033", "Enter the HMRC reference (must be less than 11 characters).")))
    )
    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("B", rowNumber, "1234"),
      Cell("C", rowNumber, "3"),
      Cell("D", rowNumber, "Yes"),
      Cell("E", rowNumber, "2:1"),
      Cell("F", rowNumber, "1234567890123.1234"),
      Cell("G", rowNumber, "12345678901.12"),
      Cell("H", rowNumber, "1234567890123.1234"),
      Cell("I", rowNumber, "123456"),
      Cell("J", rowNumber, "123456"),
      Cell("K", rowNumber, "123456"),
      Cell("L", rowNumber, "123456"),
      Cell("M", rowNumber, "123456"),
      Cell("N", rowNumber, "123456"),
      Cell("O", rowNumber, "Yes")

    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "20-12-2011"),
      Cell("B", rowNumber, "1234567"),
      Cell("C", rowNumber, "6"),
      Cell("D", rowNumber, "yess"),
      Cell("E", rowNumber, "2.5/1"),
      Cell("F", rowNumber, "1234.12345"),
      Cell("G", rowNumber, "1234.12345"),
      Cell("H", rowNumber, "1234.12345"),
      Cell("I", rowNumber, "1234567"),
      Cell("J", rowNumber, "1234567"),
      Cell("K", rowNumber, "1234567"),
      Cell("L", rowNumber, "1234567"),
      Cell("M", rowNumber, "1234567"),
      Cell("N", rowNumber, "1234567"),
      Cell("O", rowNumber, "yess"),
      Cell("P", rowNumber, "yess"),
      Cell("Q", rowNumber, "abcada123456782145")

    )
    rowData
  }

}
