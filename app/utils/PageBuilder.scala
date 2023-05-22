/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import controllers.routes
import models.CsvFiles

trait PageBuilder {

  val DEFAULT = ""

  // Schemes
  val SCHEME_CSOP: String = "csop"
  val SCHEME_EMI: String = "emi"
  val SCHEME_SAYE: String = "saye"
  val SCHEME_SIP: String = "sip"
  val SCHEME_OTHER: String = "other"
  val schemeList: Seq[String] = Seq(SCHEME_CSOP, SCHEME_EMI, SCHEME_SAYE, SCHEME_SIP, SCHEME_OTHER)

  // pageId's
  val PAGE_START = "ers_start"
  val PAGE_CHOOSE = "ers_choose"
  val PAGE_CHECK_FILE = "ers_check_file"
  val PAGE_CHECK_CSV_FILE = "ers_check_csv_file"
  val PAGE_ALT_ACTIVITY = "ers_alt_activity"
  val PAGE_ALT_AMENDS = "ers_alt_amends"
  val PAGE_GROUP_ACTIVITY = "ers_group_activity"
  val PAGE_SUMMARY_DECLARATION = "ers_summary_declaration"

  // Options
  val OPTION_YES = "1"
  val OPTION_NO = "2"
  val OPTION_UPLOAD_SPREEDSHEET = "1"
  val OPTION_NIL_RETURN = "2"
  val OPTION_ODS = "ods"
  val OPTION_CSV = "csv"

  // message file entry prefix
  val MSG_ERS: String = "ers"
  val MSG_CSOP: String = ".csop."
  val MSG_EMI: String = ".emi."
  val MSG_SAYE: String = ".saye."
  val MSG_SIP: String = ".sip."
  val MSG_OTHER: String = ".other."

  val CSOP_CSV_FILES: Int = 3
  val EMI_CSV_FILES: Int = 5
  val SAYE_CSV_FILES: Int = 3
  val SIP_CSV_FILES: Int = 2
  val OTHER_CSV_FILES: Int = 9

  val CSVFilesList: Map[String, List[CsvFiles]] = Map(
    (
      SCHEME_EMI, List(
      CsvFiles("EMI_ADJUSTMENTS"),
      CsvFiles("EMI_REPLACED"),
      CsvFiles("EMI_RCL"),
      CsvFiles("EMI_NONTAXABLE"),
      CsvFiles("EMI_TAXABLE")
    )),
    (
      SCHEME_CSOP, List(
      CsvFiles("CSOP_GRANTED"),
      CsvFiles("CSOP_RCL"),
      CsvFiles("CSOP_Exercised")
    )),
    (
      SCHEME_OTHER, List(
      CsvFiles("OTHER_GRANTS"),
      CsvFiles("OTHER_OPTIONS"),
      CsvFiles("OTHER_ACQUISITION"),
      CsvFiles("OTHER_RESTRICTED"),
      CsvFiles("OTHER_BENEFITS"),
      CsvFiles("OTHER_CONVERTABLE"),
      CsvFiles("OTHER_NOTIONAL"),
      CsvFiles("OTHER_ENCHANCEMENT"),
      CsvFiles("OTHER_SOLD")
    )),
    (
      SCHEME_SAYE, List(
      CsvFiles("SAYE_GRANTED"),
      CsvFiles("SAYE_RCL"),
      CsvFiles("SAYE_EXERCISED")
    )),
    (
      SCHEME_SIP, List(
      CsvFiles("SIP_AWARDS"),
      CsvFiles("SIP_OUT")
    ))
  )

  def getCsvFilesList(scheme: String): Seq[CsvFiles] = {
    CSVFilesList.getOrElse(scheme.toLowerCase, Seq[CsvFiles]())
  }

  def getPageElement(scheme: String, pageId: String, element: String): String = {
    scheme match {
      case SCHEME_CSOP => pageId + MSG_CSOP + element
      case SCHEME_EMI => pageId + MSG_EMI + element
      case SCHEME_SAYE => pageId + MSG_SAYE + element
      case SCHEME_SIP => pageId + MSG_SIP + element
      case SCHEME_OTHER => pageId + MSG_OTHER + element
      case _ => DEFAULT
    }
  }


  def getPageBackLink(fileType: String): String = {
    if (fileType == OPTION_ODS) {
      routes.CheckingServiceController.checkODSFilePage().toString
    } else {
      routes.CheckingServiceController.checkCSVFilePage().toString
    }
  }
}
