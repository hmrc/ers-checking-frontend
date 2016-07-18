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

package services.audit
//import models.SchemeInfo

import play.api.mvc.Request
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

object AuditEvents extends AuditEvents {
  override def auditService : AuditService = AuditService
}

trait AuditEvents {

  def auditService: AuditService

  def auditRunTimeError(exception : Throwable, contextInfo : String, sheetName : String) (implicit hc: HeaderCarrier, request: Request[_]) : Unit = {
    auditService.sendEvent("CheckingServiceRunTimeError",Map(
      "ErrorMessage" -> exception.getMessage,
      "Context" -> contextInfo,
      "sheetName" -> sheetName,
      "StackTrace" -> exception.getStackTrace.toString
    ))
  }


  def fileProcessingErrorAudit(schemeType : String, sheetName : String, errorMsg:String)(implicit hc: HeaderCarrier, request: Request[_]): Boolean = {
    auditService.sendEvent("CheckingServiceFileProcessingError", Map(
      "schemeType" -> schemeType,
      "sheetName" -> sheetName,
      "ErrorMessage" -> errorMsg)
    )
    true
  }


  def numRowsInSchemeData(sheetName : String, rowsWithData : Int)(implicit authContext: AuthContext, hc: HeaderCarrier, request: Request[_]): Boolean = {
    var empRef = ""
    if(authContext.principal.accounts.epaye.isDefined){
        empRef = authContext.principal.accounts.epaye.get.empRef.toString()
    }
    auditService.sendEvent("CheckingServiceNumRowsInSchemeData", Map(
    "sheetName" -> sheetName,
    "rowsWithData" -> rowsWithData.toString,
    "empRef" -> empRef
    ))
    true
  }

}
