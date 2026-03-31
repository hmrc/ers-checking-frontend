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

package utils

import controllers.routes

trait PageBuilder {

  val DEFAULT = ""

  // Schemes
  val SCHEME_CSOP: String = "csop"
  val SCHEME_EMI: String = "emi"
  val SCHEME_SAYE: String = "saye"
  val SCHEME_SIP: String = "sip"
  val SCHEME_OTHER: String = "other"

  // pageId's
  val PAGE_CHECK_CSV_FILE = "ers_check_csv_file"

  // Options
  val OPTION_YES = "1"
  val OPTION_NO = "2"
  val OPTION_UPLOAD_SPREEDSHEET = "1"
  val OPTION_NIL_RETURN = "2"
  val OPTION_ODS = "ods"
  val OPTION_CSV = "csv"

  // message file entry prefix
  val MSG_CSOP: String = ".csop."
  val MSG_EMI: String = ".emi."
  val MSG_SAYE: String = ".saye."
  val MSG_SIP: String = ".sip."
  val MSG_OTHER: String = ".other."

  val CSVFilesList: Map[String, List[String]] = Map(
    (
      SCHEME_EMI, List(
      "EMI_ADJUSTMENTS",
      "EMI_REPLACED",
      "EMI_RCL",
      "EMI_NONTAXABLE",
      "EMI_TAXABLE"
    )),
    (
      SCHEME_CSOP, List(
      "CSOP_GRANTED",
      "CSOP_RCL",
      "CSOP_Exercised"
    )),
    (
      SCHEME_OTHER, List(
      "OTHER_GRANTS",
      "OTHER_OPTIONS",
      "OTHER_ACQUISITION",
      "OTHER_RESTRICTED",
      "OTHER_BENEFITS",
      "OTHER_CONVERTABLE",
      "OTHER_NOTIONAL",
      "OTHER_ENCHANCEMENT",
      "OTHER_SOLD",
    )),
    (
      SCHEME_SAYE, List(
      "SAYE_GRANTED",
      "SAYE_RCL",
      "SAYE_EXERCISED"
    )),
    (
      SCHEME_SIP, List(
      "SIP_AWARDS",
      "SIP_OUT"
    ))
  )

  def getCsvFilesList(scheme: String): Seq[String] = {
    CSVFilesList.getOrElse(scheme.toLowerCase, Seq.empty[String])
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
      routes.CheckingServiceController.checkOdsFilePage().toString
    } else {
      routes.CheckingServiceController.checkCsvFilePage().toString
    }
  }
}
