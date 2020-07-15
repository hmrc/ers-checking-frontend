/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.i18n.Messages

object ContentUtil extends ContentUtil
trait ContentUtil {

  def getSchemeName(schemeType: String)(implicit messages: Messages) : (String,String) = {
    schemeType.toLowerCase match {
      case "csop" | "1" => (Messages("ers_pdf_error_report.csop"),"CSOP")
      case "emi" | "2" => (Messages("ers_pdf_error_report.emi"),"EMI")
      case "saye" | "4" => (Messages("ers_pdf_error_report.saye"),"SAYE")
      case "sip" | "5" => (Messages("ers_pdf_error_report.sip"),"SIP")
      case "other" | "3" => (Messages("ers_pdf_error_report.other"),"OTHER")
      case _ => ("","")
    }
  }

  def withArticle(data: String): String = {
    val vocals: List[Char] = List('a', 'o', 'e', 'u', 'i', 'y')
    if(vocals.contains(data.charAt(0).toLower)) {
      "an " + data
    }
    else {
      "a " + data
    }
  }
}
