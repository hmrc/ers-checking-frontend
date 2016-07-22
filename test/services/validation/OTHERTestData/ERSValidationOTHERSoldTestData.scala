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

trait ERSValidationOTHERSoldTestData {

  val rowNumber:Int = 1

  def getDescriptions: List[String] = {
    val descriptions =
      List(
      //column A
        "When dateOfEvent conforms to the expected date format, no validation error should be raised",
        "Return an error message when dateOfEvent does not conform to the expect date format",
        "Return an error message when dateOfEvent has been left empty",
      //column B
        "When inRelationToASchemeWithADOTASRef is yes or no, no validation error should be raised",
        "Return an error message when inRelationToASchemeWithADOTASRef is not a yes or no answer",
        "Return an error message when inRelationToASchemeWithADOTASRef has been left empty",
      //column C
        "When dotasRef is a correctly formatted SRN, no validation error should be raised",
        "Return an error message when dotasRef is not a number and eight digits in length",
      //column D
        "When individualOptionsPAS\\firstName is a correctly formatted first name, no validation error should be raised",
        "Return an error message when individualOptionsPAS\\firstName exceeds 35 characters",
        "Return an error message when individualOptionsPAS\\firstName has been left empty",
      //column E
        "When individualOptionsPAS\\secondName is a correctly formatted middle name, no validation error should be raised",
        "Return an error message when individualOptionsPAS\\secondName exceeds 35 characters",
      //column F
        "When individualOptionsPAS\\surname is a correctly formatted surname, no validation error should be raised",
        "Return an error message when individualOptionsPAS\\surname exceeds 35 characters",
        "Return an error message when individualOptionsPAS\\surname has been left empty",
      //column G
        "When individualOptionsPAS\\nino is a correctly formatted NINO, no validation error should be raised",
        "Return an error message when individualOptionsPAS\\nino does not conform to the expected NINO format",
      //column H
        "When individualOptionsPAS\\payeReference is a correctly formatted PAYE reference, no validation error should be raised",
        "Return an error message when individualOptionsPAS\\payeReference does not conform to the expected PAYE format",
      //column I
        "When numberOfSecuritiesOriginallyAcquired is a correctly formatted number, no validation error should be raised",
        "Return an error message when numberOfSecuritiesOriginallyAcquired does not have the correct number of decimal places",
        "Return an error message when numberOfSecuritiesOriginallyAcquired is not a number",
        "Return an error message when numberOfSecuritiesOriginallyAcquired is exceeds the maximum allowed value",
      //column J
        "When amountReceivedOnDisposal is a correctly formatted number, no validation error should be raised",
        "Return an error message when amountReceivedOnDisposal does not have 4 digits after the decimal point",
        "Return an error message when amountReceivedOnDisposal is not a number",
        "Return an error message when amountReceivedOnDisposal exceeds the maximum allowed value",
      //column K
        "When totalMarketValueOnDisposal is a correctly formatted number, no validation error should be raised",
        "Return an error message when totalMarketValueOnDisposal does not have 4 digits after the decimal point",
        "Return an error message when totalMarketValueOnDisposal is not a number",
        "Return an error message when totalMarketValueOnDisposal exceeds the maximum allowed value",
      //column L
        "When expensesIncurred is a correctly formatted number, no validation error should be raised",
        "Return an error message when expensesIncurred does not have 4 digits after the decimal point",
        "Return an error message when expensesIncurred is not a number",
        "Return an error message when expensesIncurred exceeds the maximum allowed value",
      //column M
        "When payeOperatedApplied is yes or no, no validation error should be raised",
        "Return an error message when payeOperatedApplied is not a yes or no answer",
        "Return an error message when payeOperatedApplied has been left empty",
      //column N
        "When adjusmentMadeForUKDuties is yes or no, no validation error should be raised",
        "Return an error message when adjusmentMadeForUKDuties is not a yes or no answer",
        "Return an error message when adjusmentMadeForUKDuties has been left empty"
      )
    descriptions
  }

  def getTestData: List[Cell] = {
    val testData = List(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("A", rowNumber, "12-2014-10"),
      Cell("A", rowNumber, ""),
      Cell("B", rowNumber, "Yes"),
      Cell("B", rowNumber, "Yess"),
      Cell("B", rowNumber, ""),
      Cell("C", rowNumber, "12345678"),
      Cell("C", rowNumber, "123456789"),
      Cell("D", rowNumber, "John"),
      Cell("D", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("D", rowNumber, ""),
      Cell("E", rowNumber, "S"),
      Cell("E", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("F", rowNumber, "Smith"),
      Cell("F", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("F", rowNumber, ""),
      Cell("G", rowNumber, "AB123456A"),
      Cell("G", rowNumber, "AB123456AA"),
      Cell("H", rowNumber, "123/XZ55555555"),
      Cell("H", rowNumber, "123/XZ555555556"),
      Cell("I", rowNumber, "120.33"),
      Cell("I", rowNumber, "120.1234"),
      Cell("I", rowNumber, "AbC120.33"),
      Cell("I", rowNumber, "123456789012.12"),
      Cell("J", rowNumber, "120.1234"),
      Cell("J", rowNumber, "120.12345"),
      Cell("J", rowNumber, "AbC120.33"),
      Cell("J", rowNumber, "12345678901234.1234"),
      Cell("K", rowNumber, "120.1234"),
      Cell("K", rowNumber, "120.12345"),
      Cell("K", rowNumber, "AbC120.33"),
      Cell("K", rowNumber, "12345678901234.1234"),
      Cell("L", rowNumber, "120.1234"),
      Cell("L", rowNumber, "120.12345"),
      Cell("L", rowNumber, "AbC120.33"),
      Cell("L", rowNumber, "12345678901234.1234"),
      Cell("M", rowNumber, "Yes"),
      Cell("M", rowNumber, "Yess"),
      Cell("M", rowNumber, ""),
      Cell("N", rowNumber, "Yes"),
      Cell("N", rowNumber, "Yess"),
      Cell("N", rowNumber, "")

    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      None,
      Some(List(ValidationErrorData("error.1", "001", "The date must match the yyyy-mm-dd pattern."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'1. Date of event(yyyy-mm-dd)' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.2", "002", "This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'2. Is the event in relation to a disclosable tax avoidance scheme?(yes/no)' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.3", "003", "The scheme reference number is missing."))),
      None,
      Some(List(ValidationErrorData("error.4", "004", "This entry must contain 35 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'4. Employee first name' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.5", "005", "This entry must contain 35 characters or less."))),
      None,
      Some(List(ValidationErrorData("error.6", "006", "This entry must contain 35 characters or less."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'6. Employee last name' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.7", "007", "The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter."))),
      None,
      Some(List(ValidationErrorData("error.8", "008", "PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      None,
      Some(List(ValidationErrorData("error.9" , "009", "This entry must be a number with 2 digits after the decimal point."))),
      Some(List(
        ValidationErrorData("error.9" , "009", "This entry must be a number with 2 digits after the decimal point."),
        ValidationErrorData("error.10", "010", "This entry must be a number made up of digits."),
        ValidationErrorData("error.11", "011", "This entry is larger than the maximum number value allowed."))),
      Some(List(ValidationErrorData("error.11", "011", "This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.12", "012", "This entry must be a number with 4 digits after the decimal point."))),
      Some(List(
        ValidationErrorData("error.12", "012", "This entry must be a number with 4 digits after the decimal point."),
        ValidationErrorData("error.13", "013", "This entry must be a number made up of digits."),
        ValidationErrorData("error.14", "014", "This entry is larger than the maximum number value allowed."))),
      Some(List(ValidationErrorData("error.14", "014", "This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.15", "015", "This entry must be a number with 4 digits after the decimal point."))),
      Some(List(
        ValidationErrorData("error.15", "015", "This entry must be a number with 4 digits after the decimal point."),
        ValidationErrorData("error.16", "016", "This entry must be a number made up of digits."),
        ValidationErrorData("error.17", "017", "This entry is larger than the maximum number value allowed."))),
      Some(List(ValidationErrorData("error.17", "017", "This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.18", "018", "This entry must be a number with 4 digits after the decimal point."))),
      Some(List(
        ValidationErrorData("error.18", "018", "This entry must be a number with 4 digits after the decimal point."),
        ValidationErrorData("error.19", "019", "This entry must be a number made up of digits."),
        ValidationErrorData("error.20", "020", "This entry is larger than the maximum number value allowed."))),
      Some(List(ValidationErrorData("error.20", "020", "This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.21", "021", "This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'13. Was PAYE operated?(yes/no)' must have an entry."))),
      None,
      Some(List(ValidationErrorData("error.22", "022", "This entry must be 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'14. Was any adjustment made for amounts subject to apportionment for residence or duties outside the UK?(yes/no)' must have an entry.")))
    )
    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("B", rowNumber, "Yes"),
      Cell("C", rowNumber, "12345678"),
      Cell("D", rowNumber, "John"),
      Cell("E", rowNumber, "S"),
      Cell("F", rowNumber, "Smith"),
      Cell("G", rowNumber, "AB123456A"),
      Cell("H", rowNumber, "123/XZ55555555"),
      Cell("I", rowNumber, "120.33"),
      Cell("J", rowNumber, "120.1234"),
      Cell("K", rowNumber, "120.1234"),
      Cell("L", rowNumber, "120.1234"),
      Cell("M", rowNumber, "Yes"),
      Cell("N", rowNumber, "Yes")
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "20-12-2011"),
      Cell("B", rowNumber, "Yess"),
      Cell("C", rowNumber, "123456789"),
      Cell("D", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("E", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("F", rowNumber, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"),
      Cell("G", rowNumber, "AB123456AA"),
      Cell("H", rowNumber, "123/XZ555555556"),
      Cell("I", rowNumber, "123456789012.12"),
      Cell("J", rowNumber, "120.12345"),
      Cell("K", rowNumber, "120.12345"),
      Cell("L", rowNumber, "120.12345"),
      Cell("M", rowNumber, "Yess"),
      Cell("N", rowNumber, "Yess")
    )
    rowData
  }

}
