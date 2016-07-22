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

package services.validation.CSOPTestData

import uk.gov.hmrc.services.validation.Cell
import models.ValidationErrorData

/**
 * Created by matt on 26/01/16.
 */
trait ERSValidationCSOPRCLTestData {

  val rowNumber:Int = 1

  def getDescriptions: List[String] ={
    val descriptions =
      List(
        "When dateOfEvent is correctly formatted no validation error should be raised",
        "Return The date must match the yyyy-mm-dd pattern. For dateOfEvent when an incorrect date is given",
        "When wasMoneyOrValueGiven is Yes or No then no validation error is raised",
        "Return This entry must be 'yes' or 'no'. For wasMoneyOrValueGiven when any other value is given",
        "When amtOrValue is a correctly formatted number, no validation error should be raised.",
        "Return This entry must be a number with 4 digits after the decimal point. For amtOrValue when an incorrect amount of decimal places have been entered.",
        "Return This entry must be a number made up of digits. For amtOrValue when non-number values entered",
        "Return This entry is larger than the maximum number value allowed. For amtOrValue when to many characters",
        "When releasedindividualFirstName contains characters and is less the 35 characters, no validation errors should be raised",
        "Return This entry must contain 35 characters or less. For releasedindividualFirstName when the user inputs too many characters",
        "When releasedindividualSecondName contains characters and is less the 35 characters, no validation errors should be raised",
        "Return This entry must contain 35 characters or less. For releasedindividualSecondName when the user inputs too many characters",
        "When releasedindividualLastName contains characters and is less the 35 characters, no validation errors should be raised",
        "Return This entry must contain 35 characters or less. For releasedindividualLastName when the user inputs too many characters",
        "When releasedindividualNino matches the expected Nino format, no validation error should be raised",
        "Return The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter. When the submitted text does not match a valid Nino ",
        "When releasedindividualPayeReference matches the expected PAYE reference format, no validation error should be raised",
        "Return PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters. When the submitted text does not match the PAYE format.",
        "When payeOperatedApplied is Yes or No, no validation error should be raised",
        "Return This entry must be 'yes' or 'no'. When the characters entered do not match yes or no for payeOperatedApplied."
       )
    descriptions
  }

  def getTestData: List[Cell] ={
    val testData = List(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("A", rowNumber, "aaa"),
      Cell("B", rowNumber, "Yes"),
      Cell("B",rowNumber, "yyYeesss"),
      Cell("C", rowNumber, "10.1234"),
      Cell("C", rowNumber, "10.123"),
      Cell("C", rowNumber, "Ten"),
      Cell("C", rowNumber, "123456789012345.1234"),
      Cell("D", rowNumber, "John"),
      Cell("D", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("E", rowNumber, "John"),
      Cell("E", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("F", rowNumber, "John"),
      Cell("F", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("G", rowNumber, "AB123456A"),
      Cell("G", rowNumber, "ABB25345BA1"),
      Cell("H", rowNumber, "123/XZ55555555"),
      Cell("H", rowNumber, "1234/12345/67890abcd"),
      Cell("I", rowNumber, "Yes"),
      Cell("I", rowNumber, "YyEeSs")
    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      None,
      Some(List(ValidationErrorData("error.1", "001", "Enter a date that matches the yyyy-mm-dd pattern."))),
      None,
      Some(List(ValidationErrorData("error.2", "002", "Enter 'yes' or 'no'."))),
      None,
      Some(List(ValidationErrorData("error.3", "003", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.3", "003", "Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.4", "004", "This entry must be a number made up of digits."),
        ValidationErrorData("error.5", "005", "This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.5", "005", "This entry is larger than the maximum number value allowed."))),
      None,
      Some(List(ValidationErrorData("error.6", "006", "Enter a first name (must be less than 36 characters)."))),
      None,
      Some(List(ValidationErrorData("error.7", "007", "Must be less than 36 characters."))),
      None,
      Some(List(ValidationErrorData("error.8", "008", "Enter a last name (must be less than 36 characters)."))),
      None,
      Some(List(ValidationErrorData("error.9", "009", "The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter."))),
      None,
      Some(List(ValidationErrorData("error.10", "010", "PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      None,
      Some(List(ValidationErrorData("error.11", "011", "Enter 'yes' or 'no'.")))
    )

    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2014-12-10")
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "20-12-2011")
    )
    rowData
  }

}
