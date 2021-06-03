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

package connectors

import config.ApplicationConfig

import javax.inject.{Inject, Singleton}
import models.upscan.{PreparedUpload, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.http.HttpReads.Implicits._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanConnector @Inject()(appConfig: ApplicationConfig,
                                httpClient: HttpClient
                               )(implicit ec: ExecutionContext) {

  private val headers = Map(HeaderNames.CONTENT_TYPE -> "application/json")
  private val upscanInitiateHost: String = appConfig.upscanInitiateHost
  private[connectors] val upscanInitiatePath: String = "/upscan/v2/initiate"
  private val upscanInitiateUrl: String = upscanInitiateHost + upscanInitiatePath

  def getUpscanFormData(body: UpscanInitiateRequest)
                       (implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {
    httpClient.POST[UpscanInitiateRequest, PreparedUpload](upscanInitiateUrl, body, headers.toSeq).map {
      _.toUpscanInitiateResponse
    }
  }
}
