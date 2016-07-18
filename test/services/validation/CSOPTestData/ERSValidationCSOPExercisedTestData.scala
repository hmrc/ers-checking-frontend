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

import hmrc.gsi.gov.uk.services.validation.Cell
import models.ValidationErrorData
import org.apache.commons.lang3.StringUtils

/**
 * Created by matt on 27/01/16.
 */
trait ERSValidationCSOPExercisedTestData {
  val rowNumber:Int = 1

  def getDescriptions: List[String] ={
    val descriptions =
      List(
        //A
        "When dateOfExercise conforms to the expected data format, no validation error should be raised",
        "Return The date must match the yyyy-mm-dd pattern. When the date does no follow the correct format.",
        //B
        "When individualFirstName contains 35 characters or less, no validation error should be raised",
        "Return This entry must contain 35 characters or less. When the data is larger than 35 characters for individualFirstName.",
        //C
        "When individualSecondName contains 35 characters or less, no validation error should be raised",
        "Return This entry must contain 35 characters or less. When the data is larger than 35 characters for individualSecondName.",
        //D
        "When individualLastName contains 35 characters or less, no validation error should be raised",
        "Return This entry must contain 35 characters or less. When the data is larger than 35 characters for individualLastName.",
        //E
        "When individualNino contains a correctly formatted Nino, no validation error should be raised.",
        "Return The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter. For individualNino when an incorrect Nino is provided.",
        //F
        "When individualPayeReference is correctly formatted, no validation error should be raised.",
        "Return PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters. For individualPayeReference",
        //G
        "When dateOfGrant is in the correct data format, no validation error should be raised",
        "Return The date must match the yyyy-mm-dd pattern. For dateOfGrant given an incorrect date.",
        //H
        "When numberSharesAcquired show a valid number of shares, no validation error should be raised",
        "Return This entry must be a number with 2 digits after the decimal point. When numberSharesAcquired contains an incorrect amount of decimal places.",
        "Return This entry must be a number made up of digits. When the input for numberSharesAcquired contains invalid characters.",
        "Return This entry is larger than the maximum number value allowed. When the received input is too large for numberSharesAcquired.",
        //I
        "Validate sharesPartOfLargestClass without ValidationErrors for valid data",
        "Return ValidationErrors when sharesPartOfLargestClass is given invalid data",
        "Return ValidationErrors when sharesPartOfLargestClass is given no data",
        //J
        "Validate valid sharesListedOnSE data",
        "Return ValidationErrors when sharesListedOnSE data in not valid",
        "Return ValidationErrors when sharesListedOnSE data is empty",
        //K
        "Validate valid amvPerShareAtAcquisitionDate data",
        "Return ValidationErrors when amvPerShareAtAcquisitionDate contains other than 4 decimal places",
        "Return ValidationErrors when amvPerShareAtAcquisitionDate contains non-numeric characters",
        "Return ValidationErrors when amvPerShareAtAcquisitionDate contains a number too large",
        //L
        "Validate valid exerciseValuePerShare data",
        "Return ValidationErrors when exerciseValuePerShare contains other than 4 decimal places",
        "Return ValidationErrors when exerciseValuePerShare contains non-numeric characters",
        "Return ValidationErrors when exerciseValuePerShare contains a number too large",
        //M
        "Validate valid umvPerShareAtExerciseDate data",
        "Return ValidationErrors when umvPerShareAtExerciseDate contains other than 4 decimal places",
        "Return ValidationErrors when umvPerShareAtExerciseDate contains non-numeric characters",
        "Return ValidationErrors when umvPerShareAtExerciseDate contains a number too large",
        // N
        "Validate valid mvAgreedHMRC data",
        "Return ValidationErrors when mvAgreedHMRC data in not valid",
        //O
        "Validate valid hmrcRef data",
        "Return ValidationErrors when hrmcRef is invalid",
        //P
        "Validate qualifyForTaxRelief without ValidationErrors for valid data",
        "Return ValidationErrors when qualifyForTaxRelief is given invalid data",
        "Return ValidationErrors when qualifyForTaxRelief is given no data",
        //Q
        "Validate payeOperatedApplied without ValidationErrors for valid data",
        "Return ValidationErrors when payeOperatedApplied is given invalid data",
        "Return ValidationErrors when payeOperatedApplied is given no data",
        //R
        "Validate valid deductibleAmount data",
        "Return ValidationErrors when deductibleAmount contains other than 4 decimal places",
        "Return ValidationErrors when deductibleAmount contains non-numeric characters",
        "Return ValidationErrors when deductibleAmount contains a number too large",
        // S
        "Validate valid nicsElectionAgreementEnteredInto data",
        "Return ValidationErrors when nicsElectionAgreementEnteredInto is not valid",
        "Return ValidationErrors when nicsElectionAgreementEnteredInto is empty",
        // T
        "Validate valid sharesDisposedOnSameDay data",
        "Return ValidationErrors when sharedDisposedOnSameDay is not valid",
        "Return ValidationErrors when sharedDisposedOnSameDay is empty"
      )
    descriptions
  }

  def getTestData: List[Cell] ={
    val testData = List(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("A", rowNumber, "12-2014-10"),
      Cell("B", rowNumber, "John"),
      Cell("B", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("C", rowNumber, "Jack"),
      Cell("C", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("D", rowNumber, "Jackson"),
      Cell("D", rowNumber, "AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz"),
      Cell("E", rowNumber, "AA123456A"),
      Cell("E", rowNumber, "AAAA12341.135a"),
      Cell("F", rowNumber, "123/XZ55555555"),
      Cell("F", rowNumber, "1234/12345/12341234"),
      Cell("G", rowNumber, "2014-12-10"),
      Cell("G", rowNumber, "12-2014-10"),
      Cell("H", rowNumber, "10.12"),
      Cell("H", rowNumber, "10.1234"),
      Cell("H", rowNumber, "10A.1A"),
      Cell("H", rowNumber, "1234567890123.12"),
      Cell("I", rowNumber, "yes"),
      Cell("I", rowNumber, "ys"),
      Cell("I", rowNumber, ""),
      Cell("J", rowNumber, "yes"),
      Cell("J", rowNumber, "garbage"),
      Cell("J", rowNumber, ""),
      Cell("K", rowNumber, "10.1234"),
      Cell("K", rowNumber, "10.12"),
      Cell("K", rowNumber, "abc"),
      Cell("K", rowNumber, StringUtils.leftPad("", 15, "2") + ".3434"),
      Cell("L", rowNumber, "10.4444"),
      Cell("L", rowNumber, "10.312"),
      Cell("L", rowNumber, "#!"),
      Cell("L", rowNumber, StringUtils.leftPad("", 15, "3") + ".3434"),
      Cell("M", rowNumber, "10.3412"),
      Cell("M", rowNumber, "10.12122"),
      Cell("M", rowNumber, "abc"),
      Cell("M", rowNumber, StringUtils.leftPad("", 15, "4") + ".3434"),
      Cell("N", rowNumber, "no"),
      Cell("N", rowNumber, "n"),
      Cell("O", rowNumber, "12345"),
      Cell("O", rowNumber, "o12345678901"),
      Cell("P", rowNumber, "yes"),
      Cell("P", rowNumber, "ys"),
      Cell("P", rowNumber, ""),
      Cell("Q", rowNumber, "no"),
      Cell("Q", rowNumber, "o"),
      Cell("Q", rowNumber, ""),
      Cell("R", rowNumber, "10.1234"),
      Cell("R", rowNumber, "10.12"),
      Cell("R", rowNumber, "abc"),
      Cell("R", rowNumber, StringUtils.leftPad("", 15, "2") + ".3434"),
      Cell("S", rowNumber, "yes"),
      Cell("S", rowNumber, "garbage"),
      Cell("S", rowNumber, ""),
      Cell("T", rowNumber, "yes"),
      Cell("T", rowNumber, "garbage"),
      Cell("T", rowNumber, "")

    )
    testData
  }

  def getExpectedResults: List[Option[List[ValidationErrorData]]] = {
    val expectedResults = List(
      //A
      None,
      Some(List(ValidationErrorData("error.1", "001", "Enter a date that matches the yyyy-mm-dd pattern."))),
      //B
      None,
      Some(List(ValidationErrorData("error.2", "002", "Enter a first name (must be less than 36 characters)."))),
      //C
      None,
      Some(List(ValidationErrorData("error.3", "003", "Must be less than 36 characters."))),
      //D
      None,
      Some(List(ValidationErrorData("error.4", "004", "Enter a last name (must be less than 36 characters)."))),
      //E
      None,
      Some(List(ValidationErrorData("error.5", "005", "The National Insurance number must be 2 letters followed by 6 number digits, with an optional final letter."))),
      //F
      None,
      Some(List(ValidationErrorData("error.6", "006", "PAYE reference must be a 3 digit number followed by a forward slash and up to 10 more characters."))),
      //G
      None,
      Some(List(ValidationErrorData("error.7", "007", "Enter a date that matches the yyyy-mm-dd pattern."))),
      //H
      None,
      Some(List(ValidationErrorData("error.8", "008", "Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.8", "008", "Must be a number with 2 digits after the decimal point (and no more than 11 digits in front of it)."),
        ValidationErrorData("error.9", "009", "This entry must be a number made up of digits."),
        ValidationErrorData("error.10", "010", "This entry is larger than the maximum number value allowed."))),
      Some(List(ValidationErrorData("error.10", "010", "This entry is larger than the maximum number value allowed."))),
      //I
      None,
      Some(List(ValidationErrorData("error.11", "011", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'9. Are these shares part of the largest class of shares in that company? (yes/no)' must have an entry."))),
      //J
      None,
      Some(List(ValidationErrorData("error.12", "012", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'10. Are the shares subject to the option listed on a recognised stock exchange? (yes/no)' must have an entry."))),
      //K
      None,
      Some(List(ValidationErrorData("error.13", "013","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.13", "013","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.14", "014","This entry must be a number made up of digits."),
        ValidationErrorData("error.15", "015", "This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.15", "015", "This entry is larger than the maximum number value allowed."))),
      //L
      None,
      Some(List(ValidationErrorData("error.16", "016","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.16", "016","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.17", "017","This entry must be a number made up of digits."),
        ValidationErrorData("error.18", "018", "This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.18", "018", "This entry is larger than the maximum number value allowed."))),
      //M
      None,
      Some(List(ValidationErrorData("error.19", "019","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.19", "019","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.20", "020","This entry must be a number made up of digits."),
        ValidationErrorData("error.21", "021", "This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.21", "021", "This entry is larger than the maximum number value allowed."))),
      //N
      None,
      Some(List(ValidationErrorData("error.22", "022", "Enter 'yes' or 'no'."))),
      //O
      None,
      Some(List(ValidationErrorData("error.23", "023", "Enter the HMRC reference (must be less than 11 characters)."))),
      //P
      None,
      Some(List(ValidationErrorData("error.24", "024", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'16. Does the exercise qualify for tax relief? (yes/no)' must have an entry."))),
      //Q
      None,
      Some(List(ValidationErrorData("error.25", "025", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'17. Was PAYE operated? (yes/no)' must have an entry."))),
      //R
      None,
      Some(List(ValidationErrorData("error.26", "026","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."))),
      Some(List(
        ValidationErrorData("error.26", "026","Must be a number with 4 digits after the decimal point (and no more than 13 digits in front of it)."),
        ValidationErrorData("error.27", "027","This entry must be a number made up of digits."),
        ValidationErrorData("error.28", "028", "This entry is larger than the maximum number value allowed.")
      )),
      Some(List(ValidationErrorData("error.28", "028", "This entry is larger than the maximum number value allowed."))),
      //S
      None,
      Some(List(ValidationErrorData("error.29", "029", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'19. Has a National Insurance Contributions election or agreement been operated? (yes/no)' must have an entry."))),
      //T
      None,
      Some(List(ValidationErrorData("error.30", "030", "Enter 'yes' or 'no'."))),
      Some(List(ValidationErrorData("MANDATORY", "100", "'20. Were all shares resulting from the exercise sold? (yes/no). Answer yes if they were either sold on the same day as the exercise in connection with the exercise or sale instructions were given for all shares to be sold on exercise' must have an entry.")))
      )

    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("B", rowNumber, "2014-12-10"),
      Cell("C", rowNumber, "2014-12-10"),
      Cell("D", rowNumber, "2014-12-10"),
      Cell("E", rowNumber, "2014-12-10"),
      Cell("F", rowNumber, "2014-12-10"),
      Cell("G", rowNumber, "2014-12-10"),
      Cell("H", rowNumber, "2014-12-10"),
      Cell("I", rowNumber, "2014-12-10"),
      Cell("J", rowNumber, "2014-12-10"),
      Cell("K", rowNumber, "2014-12-10"),
      Cell("L", rowNumber, "2014-12-10"),
      Cell("M", rowNumber, "2014-12-10"),
      Cell("N", rowNumber, "2014-12-10"),
      Cell("O", rowNumber, "2014-12-10"),
      Cell("P", rowNumber, "2014-12-10"),
      Cell("Q", rowNumber, "2014-12-10"),
      Cell("R", rowNumber, "2014-12-10"),
      Cell("S", rowNumber, "2014-12-10"),
      Cell("T", rowNumber, "2014-12-10")
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
      Cell("A", rowNumber, "2014-12-10"),
      Cell("B", rowNumber, "2014-12-10"),
      Cell("C", rowNumber, "2014-12-10"),
      Cell("D", rowNumber, "2014-12-10"),
      Cell("E", rowNumber, "2014-12-10"),
      Cell("F", rowNumber, "2014-12-10"),
      Cell("G", rowNumber, "2014-12-10"),
      Cell("H", rowNumber, "2014-12-10"),
      Cell("I", rowNumber, "2014-12-10"),
      Cell("J", rowNumber, "2014-12-10"),
      Cell("K", rowNumber, "2014-12-10"),
      Cell("L", rowNumber, "2014-12-10"),
      Cell("M", rowNumber, "2014-12-10"),
      Cell("N", rowNumber, "2014-12-10"),
      Cell("O", rowNumber, "2014-12-10"),
      Cell("P", rowNumber, "2014-12-10"),
      Cell("Q", rowNumber, "2014-12-10"),
      Cell("R", rowNumber, "2014-12-10"),
      Cell("S", rowNumber, "2014-12-10"),
      Cell("T", rowNumber, "2014-12-10")
    )
    rowData
  }

}
