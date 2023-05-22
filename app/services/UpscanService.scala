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

package services

import config.ApplicationConfig
import connectors.UpscanConnector
import javax.inject.{Inject, Singleton}
import models.upscan.{UploadId, UpscanIds, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class UpscanService @Inject()(upscanConnector: UpscanConnector, appConfig: ApplicationConfig) {

  def urlToString(c: Call): String = redirectUrlBase + c.url
  def callbackCsv(uploadId: UploadId)(implicit sessionId: String): Call = controllers.internal.routes.UpscanCallbackController.callbackCsv(uploadId, sessionId)
  def callbackOds(implicit sessionId: String): Call = controllers.internal.routes.UpscanCallbackController.callbackOds(sessionId)
  def successCsv(uploadId: UploadId, scheme: String): String = urlToString(controllers.routes.UpscanController.successCSV(uploadId, scheme))
  def successOds(scheme: String): String = urlToString(controllers.routes.UpscanController.successODS(scheme))

  lazy val isSecure: Boolean = appConfig.upscanProtocol == "https"
  lazy val redirectUrlBase: String = appConfig.upscanRedirectBase

  def getUpscanFormData(isCSV: Boolean, scheme: String, upscanId: Option[UpscanIds] = None)
                       (implicit hc: HeaderCarrier, request: Request[_]): Future[UpscanInitiateResponse] = {

    implicit val sessionId: String = hc.sessionId.get.value

    def isCsvAndUploadId = isCSV && upscanId.isDefined

    val callback = if (isCsvAndUploadId) callbackCsv(upscanId.get.uploadId) else callbackOds
    val success = if (isCsvAndUploadId) successCsv(upscanId.get.uploadId, scheme) else successOds(scheme)
    val failure = urlToString(controllers.routes.UpscanController.failure())
    val upscanInitiateRequest: UpscanInitiateRequest =
      if (isCsvAndUploadId) {
        UpscanInitiateRequest(callback.absoluteURL(isSecure), success, failure, Some(1))
      } else {
        UpscanInitiateRequest(callback.absoluteURL(isSecure), success, failure, Some(1), Some(10000000))
      }
    upscanConnector.getUpscanFormData(upscanInitiateRequest)
  }
}
