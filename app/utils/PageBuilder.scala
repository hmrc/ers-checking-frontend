/*
 * Copyright 2017 HM Revenue & Customs
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
import controllers.routes
import play.api.mvc.Call

object PageBuilder extends PageBuilder
trait PageBuilder {

  val DEFAULT = ""

  // Schemes
  val SCHEME_CSOP: String = "1"
  val SCHEME_EMI: String = "2"
  val SCHEME_SAYE: String = "4"
  val SCHEME_SIP: String = "5"
  val SCHEME_OTHER: String = "3"

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

  def getGlobalPageElement(scheme: String, element: String) : String = {
    val pageElement: String = scheme match {
      case SCHEME_CSOP => Messages(MSG_ERS + MSG_CSOP + element)
      case SCHEME_EMI => Messages(MSG_ERS + MSG_EMI + element)
      case SCHEME_SAYE => Messages(MSG_ERS + MSG_SAYE + element)
      case SCHEME_SIP => Messages(MSG_ERS + MSG_SIP + element)
      case SCHEME_OTHER => Messages(MSG_ERS + MSG_OTHER + element)
      case _ => DEFAULT
    }
    pageElement
  }

  def getPageElement(scheme: String, pageId: String, element: String, para: String = "") : String = {
    val pageElement: String = scheme match {
      case SCHEME_CSOP => Messages(pageId + MSG_CSOP + element, para)
      case SCHEME_EMI => Messages(pageId + MSG_EMI + element, para)
      case SCHEME_SAYE => Messages(pageId + MSG_SAYE + element, para)
      case SCHEME_SIP => Messages(pageId + MSG_SIP + element, para)
      case SCHEME_OTHER => Messages(pageId + MSG_OTHER + element, para)
      case _ => DEFAULT
    }
    pageElement
  }


  def getPageBackLink(fileType: String) : String = {
    fileType match {
      case OPTION_ODS => routes.CheckingServiceController.checkODSFilePage.toString()
      case OPTION_CSV => routes.CheckingServiceController.checkCSVFilePage.toString()
    }
  }

  /*

  // action = getPageFormAction(scheme, pageId)
  def getPageFormAction(scheme: String, pageId: String) : Call = {
    val formAction: Call = scheme match {
      	case SCHEME_CSOP => {
      	  pageId match {
      	    case PAGE_ALT_ACTIVITY => routes.ReturnServiceController.altActivitySelected
      	  }
      	} 
    }
    formAction
  } 
  
  */

  def getNumberOfBrowseButtons(scheme: String): Int = {
    val maxBrowseButtons: Int = scheme match {
      case SCHEME_CSOP => CSOP_CSV_FILES
      case SCHEME_EMI => EMI_CSV_FILES
      case SCHEME_SAYE => SAYE_CSV_FILES
      case SCHEME_SIP => SIP_CSV_FILES
      case SCHEME_OTHER => OTHER_CSV_FILES
      case _ => 1
    }
    maxBrowseButtons
  }


}
