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

package connectors

import config.{ApplicationConfig, WSHttp}
import models.upscan.{PreparedUpload, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}

import scala.concurrent.{ExecutionContext, Future}

trait UpscanConnector {
	val httpPost: HttpPost
	val headers: Map[String, String]
	val upscanInitiateHost: String
	val upscanInitiatePath: String
	val upscanInitiateUrl: String

	def getUpscanFormData(body: UpscanInitiateRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpscanInitiateResponse]
}

class UpscanConnectorImpl extends UpscanConnector {

	val httpPost: HttpPost = WSHttp

	val headers = Map(
		HeaderNames.CONTENT_TYPE -> "application/json"
	)

	val upscanInitiateHost: String = ApplicationConfig.upscanInitiateHost
	val upscanInitiatePath: String = "/upscan/v2/initiate"
	val upscanInitiateUrl: String = upscanInitiateHost + upscanInitiatePath

	def getUpscanFormData(body: UpscanInitiateRequest)
											 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpscanInitiateResponse] = {
		httpPost.POST[UpscanInitiateRequest, PreparedUpload](upscanInitiateUrl, body, headers.toSeq).map {
			_.toUpscanInitiateResponse
		}
	}
}

object UpscanConnector extends UpscanConnectorImpl
