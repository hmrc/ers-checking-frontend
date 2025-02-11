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

package services.audit

import controllers.auth.RequestWithOptionalEmpRef
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditEvents @Inject()(val auditConnector: AuditConnector)(implicit val ec: ExecutionContext) extends AuditService {

  def auditFileSize(fileSize: String)(implicit hc: HeaderCarrier): Future[AuditResult] =
    sendEvent(
      "UploadFileSizeFromUpscanCallback",
      Map(
        "fileSize" -> fileSize
      )
    )

  def auditRunTimeError(exception : Throwable, contextInfo : String, sheetName : String)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    sendEvent("CheckingServiceRunTimeError", Map(
      "ErrorMessage" -> exception.getMessage,
      "Context" -> contextInfo,
      "sheetName" -> sheetName,
      "StackTrace" -> exception.getStackTrace.toString
    ))
  }

  def fileProcessingErrorAudit(schemeType : String, sheetName : String, errorMsg:String)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    sendEvent("CheckingServiceFileProcessingError", Map(
      "schemeType" -> schemeType,
      "sheetName" -> sheetName,
      "ErrorMessage" -> errorMsg)
    )
  }

  def numRowsInSchemeData(sheetName : String, rowsWithData : Int)
                         (implicit hc: HeaderCarrier, request: RequestWithOptionalEmpRef[_], ec: ExecutionContext): Future[AuditResult] = {
    val empRef = request.optionalEmpRef.map(_.value).getOrElse("")
    sendEvent("CheckingServiceNumRowsInSchemeData", Map(
    "sheetName" -> sheetName,
    "rowsWithData" -> rowsWithData.toString,
    "empRef" -> empRef
    ))
  }

}
