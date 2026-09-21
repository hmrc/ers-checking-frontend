/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import services.XMLTestHelpers.{buildValidOdsXml, closeTable, openTable}

import java.io.ByteArrayInputStream

object CsopV4V5XMLTestData {

  // ---------------------------- CSOP V4 test data ----------------------------

  // --------------------------- CSOP_OptionsGranted_V4 ----------------------------

  val csopOptionsGrantedV4SheetName = "CSOP_OptionsGranted_V4"

  val csopOptionsGrantedV4Row1 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce2' office:value-type='string'><text:p>CSOP scheme template – Options granted</text:p></table:table-cell><table:table-cell table:style-name='ce3' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row2 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' office:value-type='string'><text:p>How to complete this schedule:</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row3 =
    <table:table-row table:style-name='ro2'><table:table-cell table:style-name='ce6' office:value-type='string'><text:p>Enter details of each employee and the Company Share Option Plan (CSOP) below.</text:p><text:p><text:span text:style-name='T1'>Please note</text:span>: Monetary values must be entered to 4 decimal places in pounds sterling.</text:p><text:p>Numbers of shares and securities must be entered to 2 decimal places.</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row4 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' office:value-type='string'><text:p>Important note</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row5 =
    <table:table-row table:style-name='ro3'><table:table-cell table:style-name='ce14' table:number-columns-spanned='8' table:number-rows-spanned='1' office:value-type='string'><text:p>You<text:s></text:s><text:span text:style-name='T4'>must not</text:span><text:s></text:s>alter the structure or formatting of this schedule. If you do your schedule will be rejected.</text:p><text:p>Please note when completing the template if you leave 10 or more consecutive rows blank with no data populated, It will be assumed that there is no further information contained below these blank rows within this sheet. Therefore it is important you do not leave blank rows between the rows of data you are reporting. Note the system will continue to check for information on subsequent sheets within the same template.</text:p><text:p></text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='7'></table:covered-table-cell><table:table-cell table:style-name='ce5'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row6 =
    <table:table-row table:style-name='ro4'><table:table-cell table:style-name='ce6' office:value-type='string'><text:p>For more information on completing this schedule, follow the link below. You must be connected to the internet to access the guide.</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row7 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce8' office:value-type='string'><text:p><text:a xlink:href='https://www.gov.uk/government/publications/company-share-option-plan-end-of-year-return-template'>CSOP guidance</text:a></text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row8 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce9' table:number-columns-repeated='9'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row9 =
    <table:table-row table:style-name='ro5'><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>1.</text:p><text:p>Date of grant</text:p><text:p>(yyyy-mm-dd)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>2.</text:p><text:p>Number of employees granted options</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>3.</text:p><text:p>Over how many shares in total were CSOP options granted</text:p><text:p>e.g. 100.00</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>4.</text:p><text:p>Unrestricted market value (UMV) of each share used to determine option exercise price</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>5.</text:p><text:p>Option exercise price per share</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>6.</text:p><text:p>Are the shares under the CSOP option listed on a recognised stock exchange?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>7.</text:p><text:p>If no, was the market value agreed with HMRC?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>8.</text:p><text:p>If yes, enter the HMRC valuation reference given</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>9.</text:p><text:p>Using the UMV at the time of each relevant grant, does any employee hold unexercised CSOP options over shares totalling more than £30k, including this grant?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row10 =
    <table:table-row table:style-name='ro1'><table:table-cell office:date-value='2015-09-23T00:00:00' table:style-name='ce10' office:value-type='date'><text:p>2015-09-23</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='250'><text:p>250</text:p></table:table-cell><table:table-cell table:style-name='ce11' office:value-type='float' office:value='123.12'><text:p>123.12</text:p></table:table-cell><table:table-cell table:style-name='ce12' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce12' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce13' office:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell table:style-name='ce13' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce13' office:value-type='string'><text:p>AB12345678</text:p></table:table-cell><table:table-cell table:style-name='ce13' office:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4Row11 =
    <table:table-row table:style-name='ro6' table:number-rows-repeated='1048566'><table:table-cell table:number-columns-repeated='16384'></table:table-cell></table:table-row>

  val csopOptionsGrantedV4XML =
    openTable(csopOptionsGrantedV4SheetName) + csopOptionsGrantedV4Row1 + csopOptionsGrantedV4Row2 +
      csopOptionsGrantedV4Row3 + csopOptionsGrantedV4Row4 + csopOptionsGrantedV4Row5 +
      csopOptionsGrantedV4Row6 + csopOptionsGrantedV4Row7 + csopOptionsGrantedV4Row8 + csopOptionsGrantedV4Row9 +
      csopOptionsGrantedV4Row10 + csopOptionsGrantedV4Row11 + closeTable

  // --------------------------- CSOP_OptionsRCL_V4_V5 ----------------------------

  val csopOptionsRCLV4V5Row1 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce2' office:value-type='string'><text:p>CSOP scheme template – Options released (including exchanges) cancelled or lapsed in year</text:p></table:table-cell><table:table-cell table:style-name='ce2' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row2 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' office:value-type='string'><text:p>How to complete this schedule:</text:p></table:table-cell><table:table-cell table:style-name='ce4' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row3 =
    <table:table-row table:style-name='ro7'><table:table-cell table:style-name='ce6' office:value-type='string'><text:p>Enter details of each employee who received money or value on the release, cancellation or lapse of the option.</text:p><text:p><text:span text:style-name='T1'>Please note</text:span>: Monetary values must be entered to 4 decimal places in pounds sterling.</text:p><text:p></text:p></table:table-cell><table:table-cell table:style-name='ce6' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row4 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' office:value-type='string'><text:p>Important note</text:p></table:table-cell><table:table-cell table:style-name='ce4' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row5 =
    <table:table-row table:style-name='ro8'><table:table-cell table:style-name='ce14' table:number-columns-spanned='7' table:number-rows-spanned='1' office:value-type='string'><text:p>You<text:s></text:s><text:span text:style-name='T4'>must not</text:span><text:s></text:s>alter the structure or formatting of this schedule. If you do your schedule will be rejected.</text:p><text:p>Please note when completing the template if you leave 10 or more consecutive rows blank with no data populated, It will be assumed that there is no further information contained below these blank rows within this sheet. Therefore it is important you do not leave blank rows between the rows of data you are reporting. Note the system will continue to check for information on subsequent sheets within the same template.</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='6'></table:covered-table-cell><table:table-cell table:style-name='ce6' table:number-columns-repeated='2'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row6 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce6' office:value-type='string'><text:p>For more information on completing this schedule, follow the link below. You must be connected to the internet to access the guide.</text:p></table:table-cell><table:table-cell table:style-name='ce6' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row7 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce15' office:value-type='string'><text:p><text:a xlink:href='https://www.gov.uk/government/publications/company-share-option-plan-end-of-year-return-template'>CSOP guidance</text:a></text:p></table:table-cell><table:table-cell table:style-name='ce15' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row8 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce9' table:number-columns-repeated='2'></table:table-cell><table:table-cell table:style-name='ce12'></table:table-cell><table:table-cell table:style-name='ce9' table:number-columns-repeated='6'></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row9 =
    <table:table-row table:style-name='ro9'><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>1.</text:p><text:p>Date of event</text:p><text:p>(yyyy-mm-dd)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>2.</text:p><text:p>Was money or value received by the option holder or anyone else when the option was released, exchanged, cancelled or lapsed?</text:p><text:p>(yes/no)</text:p><text:p>If yes go to question 3, otherwise no further information is needed for this event.</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>3.</text:p><text:p>If yes, amount or value</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>4.</text:p><text:p>Employee first name</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>5.</text:p><text:p>Employee second name</text:p><text:p>(if applicable)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>6.</text:p><text:p>Employee last name</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>7.</text:p><text:p>National Insurance number</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>8.</text:p><text:p>PAYE reference of employing company</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>9.</text:p><text:p>Was PAYE operated?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row10 =
    <table:table-row table:style-name='ro1'><table:table-cell office:date-value='2015-10-09T00:00:00' table:style-name='ce10' office:value-type='date'><text:p>2015-10-09</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce12' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Jonathan</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Riley</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Stinston</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>AB123456A</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>123/CD1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16375'></table:table-cell></table:table-row>

  val csopOptionsRCLV4V5Row11 =
    <table:table-row table:style-name='ro6' table:number-rows-repeated='1048566'><table:table-cell table:number-columns-repeated='16384'></table:table-cell></table:table-row>

  // --------------------------- CSOP_OptionsExercised_V4_V5 ----------------------------

  val csopOptionsExercisedV4V5Row1 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce18' table:number-columns-spanned='20' table:number-rows-spanned='1' office:value-type='string'><text:p>CSOP scheme template – Options and replacement options exercised</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='19'></table:covered-table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row2 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce19' table:number-columns-spanned='20' table:number-rows-spanned='1' office:value-type='string'><text:p>How to complete this schedule:</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='19'></table:covered-table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row3 =
    <table:table-row table:style-name='ro10'><table:table-cell table:style-name='ce14' table:number-columns-spanned='20' table:number-rows-spanned='1' office:value-type='string'><text:p>Enter details of each employee and the Company Share Option Plan (CSOP) below.</text:p><text:p><text:span text:style-name='T1'>Please note</text:span>: Monetary values must be entered to 4 decimal places in pounds sterling.</text:p><text:p>Numbers of shares and securities must be entered to 2 decimal places.</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='19'></table:covered-table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row4 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce19' table:number-columns-spanned='20' table:number-rows-spanned='1' office:value-type='string'><text:p>Important note</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='19'></table:covered-table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row5 =
    <table:table-row table:style-name='ro11'><table:table-cell table:style-name='ce14' table:number-columns-spanned='20' table:number-rows-spanned='1' office:value-type='string'><text:p>You<text:s></text:s><text:span text:style-name='T4'>must not</text:span><text:s></text:s>alter the structure or formatting of this schedule. If you do your schedule will be rejected.</text:p><text:p>Please note when completing the template if you leave 10 or more consecutive rows blank with no data populated, It will be assumed that there is no further information contained below these blank rows within this sheet.</text:p><text:p>Therefore it is important you do not leave blank rows between the rows of data you are reporting. Note the system will continue to check for information on subsequent sheets within the same template.</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='19'></table:covered-table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row6 =
    <table:table-row table:style-name='ro12'><table:table-cell table:style-name='ce14' table:number-columns-spanned='12' table:number-rows-spanned='1' office:value-type='string'><text:p>For more information on completing this schedule, follow the link below. You must be connected to the internet to access the guide.</text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='11'></table:covered-table-cell><table:table-cell table:style-name='ce16' table:number-columns-repeated='8'></table:table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row7 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce20' table:number-columns-spanned='10' table:number-rows-spanned='1' office:value-type='string'><text:p><text:a xlink:href='https://www.gov.uk/government/publications/company-share-option-plan-end-of-year-return-template'>CSOP guidance</text:a></text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='9'></table:covered-table-cell><table:table-cell table:style-name='ce17'></table:table-cell><table:table-cell table:style-name='ce16' table:number-columns-repeated='3'></table:table-cell><table:table-cell table:style-name='ce17' table:number-columns-repeated='4'></table:table-cell><table:table-cell table:style-name='ce16'></table:table-cell><table:table-cell table:style-name='ce17'></table:table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row8 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce9' table:number-columns-repeated='20'></table:table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row9 =
    <table:table-row table:style-name='ro13'><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>1.</text:p><text:p>Date of event</text:p><text:p>(yyyy-mm-dd)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>2.</text:p><text:p>Employee first name</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>3.</text:p><text:p>Employee second name</text:p><text:p>(if applicable)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>4.</text:p><text:p>Employee last name</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>5.</text:p><text:p>National Insurance number</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>6.</text:p><text:p>PAYE reference of employing company</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>7.</text:p><text:p>Date of grant</text:p><text:p>(yyyy-mm-dd)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>8.</text:p><text:p>Total number of shares employee entitled to on exercise of the option before any cashless exercise or other adjustment</text:p><text:p>e.g. 100.00</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>9.</text:p><text:p>Are these shares part of the largest class of shares in that company?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>10.</text:p><text:p>Are the shares subject to the option listed on a recognised stock exchange?</text:p><text:p>(yes/no)</text:p><text:p></text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>11.</text:p><text:p>Actual market value (AMV) of a share on the date of exercise</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>12.</text:p><text:p>Exercise price per share</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>13.</text:p><text:p>Unrestricted market value (UMV) of a share on the date of exercise</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>14.</text:p><text:p>If the answer to question 10 is no, was the market value agreed with HMRC?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>15.</text:p><text:p>If yes, enter the HMRC valuation reference given</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>16.</text:p><text:p>Does the exercise qualify for tax relief?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>17.</text:p><text:p>Was PAYE operated?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>18.</text:p><text:p>If yes, deductible amount</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>19.</text:p><text:p>Has a National Insurance Contributions election or agreement been operated?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' office:value-type='string'><text:p>20.</text:p><text:p>Were all shares resulting from the exercise sold? (yes/no). Answer yes if they were either sold on the same day as the exercise in connection with the exercise or sale instructions were given for all shares to be sold on exercise</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row10 =
    <table:table-row table:style-name='ro1'><table:table-cell office:date-value='2015-07-23T00:00:00' table:style-name='ce10' office:value-type='date'><text:p>2015-07-23</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Jerry</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Daniel</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>Springer</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>AB123456A</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>123/CD1234</text:p></table:table-cell><table:table-cell office:date-value='2015-08-29T00:00:00' table:style-name='ce10' office:value-type='date'><text:p>2015-08-29</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='123.12'><text:p>123.12</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>AB12345678</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='float' office:value='12.1234'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce9' office:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:number-columns-repeated='16364'></table:table-cell></table:table-row>

  val csopOptionsExercisedV4V5Row11 =
    <table:table-row table:style-name='ro6' table:number-rows-repeated='1048566'><table:table-cell table:number-columns-repeated='16384'></table:table-cell></table:table-row>

  def getValidCSOPV4DataStream: ByteArrayInputStream = {
    val csopV4BodyXml: String =
      csopOptionsGrantedV4XML + csopOptionsRCLV4V5("CSOP_OptionsRCL_V4") + csopOptionsExercisedV4V5(
        "CSOP_OptionsExercised_V4"
      )
    new ByteArrayInputStream(buildValidOdsXml(csopV4BodyXml).getBytes("utf-8"))
  }

  // ---------------------------- CSOP V5 test data ----------------------------

  // --------------------------- CSOP_OptionsGranted_V5 ----------------------------

  val csopOptionsGrantedV5SheetName = "CSOP_OptionsGranted_V5"

  val csopOptionsGrantedV5Row1 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce2' calcext:value-type='string'><text:p>CSOP scheme template – Options granted</text:p></table:table-cell><table:table-cell table:style-name='ce3' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row2 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' calcext:value-type='string'><text:p>How to complete this schedule:</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row3 =
    <table:table-row table:style-name='ro2'><table:table-cell table:style-name='ce6' calcext:value-type='string'><text:p>Enter details of each employee and the Company Share Option Plan (CSOP) below.</text:p><text:p><text:span text:style-name='T1'>Please note</text:span>: Monetary values must be entered to 4 decimal places in pounds sterling.</text:p><text:p>Numbers of shares and securities must be entered to 2 decimal places.</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row4 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce4' calcext:value-type='string'><text:p>Important note</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row5 =
    <table:table-row table:style-name='ro3'><table:table-cell table:style-name='ce14' table:number-columns-spanned='8' table:number-rows-spanned='1' calcext:value-type='string'><text:p>You <text:span text:style-name='T2'>must not</text:span> alter the structure or formatting of this schedule. If you do your schedule will be rejected.</text:p><text:p>Please note when completing the template if you leave 10 or more consecutive rows blank with no data populated, It will be assumed that there is no further information contained below these blank rows within this sheet. Therefore it is important you do not leave blank rows between the rows of data you are reporting. Note the system will continue to check for information on subsequent sheets within the same template.</text:p><text:p></text:p></table:table-cell><table:covered-table-cell table:number-columns-repeated='7'></table:covered-table-cell><table:table-cell table:style-name='ce5'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row6 =
    <table:table-row table:style-name='ro4'><table:table-cell table:style-name='ce6' calcext:value-type='string'><text:p>For more information on completing this schedule, follow the link below. You must be connected to the internet to access the guide.</text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row7 =
    <table:table-row table:style-name='ro1'><table:table-cell table:style-name='ce8' calcext:value-type='string'><text:p><text:a xlink:href='https://www.gov.uk/government/publications/company-share-option-plan-end-of-year-return-template' xlink:type='simple'>CSOP guidance</text:a></text:p></table:table-cell><table:table-cell table:style-name='ce5' table:number-columns-repeated='8'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row8 =
    <table:table-row table:style-name='ro1'><table:table-cell table:number-columns-repeated='9'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row9 =
    <table:table-row table:style-name='ro5'><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>1.</text:p><text:p>Date of grant</text:p><text:p>(yyyy-mm-dd)</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>2.</text:p><text:p>Number of employees granted options</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>3.</text:p><text:p>Over how many shares in total were CSOP options granted</text:p><text:p>e.g. 100.00</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>4.</text:p><text:p>Unrestricted market value (UMV) of each share used to determine option exercise price</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>5.</text:p><text:p>Option exercise price per share</text:p><text:p>£</text:p><text:p>e.g. 10.1234</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>6.</text:p><text:p>Are the shares under the CSOP option listed on a recognised stock exchange?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>7.</text:p><text:p>If no, was the market value agreed with HMRC?</text:p><text:p>(yes/no)</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>8.</text:p><text:p>If yes, enter the HMRC valuation reference given</text:p></table:table-cell><table:table-cell table:style-name='ce7' calcext:value-type='string'><text:p>9.</text:p><text:p>Using the UMV at the time of each relevant grant, does any employee hold unexercised CSOP options over shares totalling more than £60k, including this grant?</text:p><text:p>(yes/no)</text:p></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row10 =
    <table:table-row table:style-name='ro1'><table:table-cell office:date-value='2015-09-23' table:style-name='ce10' calcext:value-type='date'><text:p>2015-09-23</text:p></table:table-cell><table:table-cell calcext:value-type='float' office:value='250'><text:p>250</text:p></table:table-cell><table:table-cell table:style-name='ce11' calcext:value-type='float' office:value='123.12'><text:p>123.12</text:p></table:table-cell><table:table-cell table:style-name='ce12' calcext:value-type='float' office:value='12.1234' table:number-columns-repeated='2'><text:p>12.1234</text:p></table:table-cell><table:table-cell table:style-name='ce13' calcext:value-type='string'><text:p>no</text:p></table:table-cell><table:table-cell table:style-name='ce13' calcext:value-type='string'><text:p>yes</text:p></table:table-cell><table:table-cell table:style-name='ce13' calcext:value-type='string'><text:p>AB12345678</text:p></table:table-cell><table:table-cell table:style-name='ce13' calcext:value-type='string'><text:p>no</text:p></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row11 =
    <table:table-row table:style-name='ro6' table:number-rows-repeated='1048565'><table:table-cell table:number-columns-repeated='9'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5Row12 =
    <table:table-row table:style-name='ro6'><table:table-cell table:number-columns-repeated='9'></table:table-cell></table:table-row>

  val csopOptionsGrantedV5XML =
    openTable(csopOptionsGrantedV5SheetName) + csopOptionsGrantedV5Row1 + csopOptionsGrantedV5Row2 +
      csopOptionsGrantedV5Row3 + csopOptionsGrantedV5Row4 + csopOptionsGrantedV5Row5 +
      csopOptionsGrantedV5Row6 + csopOptionsGrantedV5Row7 + csopOptionsGrantedV5Row8 + csopOptionsGrantedV5Row9 +
      csopOptionsGrantedV5Row10 + csopOptionsGrantedV5Row11 + csopOptionsGrantedV5Row12 + closeTable

  def getValidCSOPV5DataStream: ByteArrayInputStream = {
    val csopV5BodyXml: String =
      csopOptionsGrantedV5XML + csopOptionsRCLV4V5("CSOP_OptionsRCL_V5") + csopOptionsExercisedV4V5(
        "CSOP_OptionsExercised_V5"
      )
    new ByteArrayInputStream(buildValidOdsXml(csopV5BodyXml).getBytes("utf-8"))
  }

  def csopOptionsRCLV4V5(sheetName: String): String =
    openTable(sheetName) + csopOptionsRCLV4V5Row1 + csopOptionsRCLV4V5Row2 +
      csopOptionsRCLV4V5Row3 + csopOptionsRCLV4V5Row4 + csopOptionsRCLV4V5Row5 +
      csopOptionsRCLV4V5Row6 + csopOptionsRCLV4V5Row7 + csopOptionsRCLV4V5Row8 + csopOptionsRCLV4V5Row9 + csopOptionsRCLV4V5Row10 + csopOptionsRCLV4V5Row11 + closeTable

  def csopOptionsExercisedV4V5(sheetName: String): String =
    openTable(sheetName) + csopOptionsExercisedV4V5Row1 + csopOptionsExercisedV4V5Row2 +
      csopOptionsExercisedV4V5Row3 + csopOptionsExercisedV4V5Row4 + csopOptionsExercisedV4V5Row5 +
      csopOptionsExercisedV4V5Row6 + csopOptionsExercisedV4V5Row7 + csopOptionsExercisedV4V5Row8 + csopOptionsExercisedV4V5Row9 + csopOptionsExercisedV4V5Row10 + csopOptionsExercisedV4V5Row11 + closeTable

}
