/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}

trait AuditService {
  val auditSource = "ers-checking-frontend"

  def auditConnector : AuditConnector

  def sendEvent(transactionName : String, details: Map[String, String])
               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] =
    auditConnector.sendEvent(buildEvent(transactionName, details))

  private[audit] def buildEvent(transactionName: String,  details: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = auditSource,
      auditType = transactionName,
      tags = generateTags(hc),
      detail = details
    )


  private[audit] def generateTags(hc: HeaderCarrier): Map[String, String] =


    hc.headers(HeaderNames.explicitlyIncludedHeaders).toMap ++ hc.extraHeaders.toMap ++ hc.otherHeaders.toMap ++ Map("dateTime" ->  getDateTime.toString)

    private def getDateTime = java.time.LocalTime.now()

}
