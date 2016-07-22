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

trait ERSValidationOTHEROtherBenefitsTestData {

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
        "validate individualPAO\\firstName without ValidationErrors for valid data",
        "validate individualPAO\\firstName with ValidationErrors for a string too long",
        "validate individualPAO\\firstName with ValidationErrors for an empty field",
        //E
        "validate individualPAO\\secondName without ValidationErrors for valid data",
        "validate individualPAO\\secondName without ValidationErrors for a string too long",
        //F
        "validate individualPAO\\surname without ValidationErrors for valid data",
        "validate individualPAO\\surname with ValidationErrors for a string too long",
        "validate individualPAO\\surname with ValidationErrors for an empty field",
        //G
        "validate individualPAO\\nino without ValidationErrors for valid data",
        "validate individualPAO\\nino with ValidationErrors for invalid data",
        //H
        "validate individualPAO\\payeReference without ValidationErrors for valid data",
        "validate individualPAO\\payeReference with ValidationErrors for invalid data",
        //I
        "validate dateSecuritiesOriginallyAcquired without ValidationErrors for valid data",
        "validate dateSecuritiesOriginallyAcquired with ValidationErrors for invalid data",
        "validate dateSecuritiesOriginallyAcquired with ValidationErrors for an empty field",
        //J
        "validate numberOfSecuritiesOriginallyAcquiredamountOrMarketValueOfTheBenefit without ValidationErrors for valid data",
        "validate numberOfSecuritiesOriginallyAcquiredamountOrMarketValueOfTheBenefit with ValidationErrors for invalid data",
        //K
        "validate amountOrMarketValueOfTheBenefit without ValidationErrors for valid data",
        "validate amountOrMarketValueOfTheBenefit with ValidationErrors for invalid data",
        //L
        "validate payeOperatedApplied without ValidationErrors for valid data",
        "validate payeOperatedApplied with ValidationErrors for a invalid data",
        "validate payeOperatedApplied with ValidationErrors for an empty field",
        //M
        "validate adjusmentMadeForUKDuties without ValidationErrors for valid data",
        "validate adjusmentMadeForUKDuties with ValidationErrors for a invalid data",
        "validate adjusmentMadeForUKDuties with ValidationErrors for an empty field"
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
      Cell("D",rowNumber,"Guss"),
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
      Cell("H",rowNumber,"123XZ555555??"),
      Cell("I",rowNumber,"2014-08-30"),
      Cell("I",rowNumber,"2014-08/30"),
      Cell("I",rowNumber,""),
      Cell("J",rowNumber,"12345678911.12"),
      Cell("J",rowNumber,"123456789111.123"),
      Cell("K",rowNumber,"1234567891123.1234"),
      Cell("K",rowNumber,"12345678911235.12345"),
      Cell("L",rowNumber,"yes"),
      Cell("L",rowNumber,"yav"),
      Cell("L",rowNumber,""),
      Cell("M",rowNumber,"yes"),
      Cell("M",rowNumber,"yav"),
      Cell("M",rowNumber,"")
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
      Some(List(ValidationErrorData("error.9","009","The date must match the yyyy-mm-dd pattern."))),
      Some(List(ValidationErrorData("MANDATORY","100","'9. Date securities originally acquired (yyyy-mm-dd)' must have an entry."))),
      //J
      None,
      Some(List(ValidationErrorData("error.10","010","This entry must be a number with 2 digits after the decimal point."))),
      //K
      None,
      Some(List(ValidationErrorData("error.11","011","This entry must be a number with 4 digits after the decimal point."))),
      //L
      None,
      Some(List(ValidationErrorData("error.12","012","This entry must be \'yes\' or \'no\'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'12. Was PAYE operated? (yes/no)' must have an entry."))),
      //M|
      None,
      Some(List(ValidationErrorData("error.13","013","This entry must be \'yes\' or \'no\'."))),
      Some(List(ValidationErrorData("MANDATORY","100","'13. Was any adjustment made for amounts subject to apportionment for residence or duties outside the UK (yes/no)' must have an entry.")))
    )
    expectedResults
  }

  def getValidRowData:Seq[Cell] = {
    val rowData = Seq(
    )
    rowData
  }

  def getInvalidRowData:Seq[Cell] = {
    val rowData = Seq(
    )
    rowData
  }

}
