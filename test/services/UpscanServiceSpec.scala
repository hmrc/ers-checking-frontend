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

package services

import config.ApplicationConfig
import connectors.{UpscanConnector, UpscanConnectorImpl}
import models.upscan.{NotStarted, Reference, UploadId, UpscanIds, UpscanInitiateRequest, UpscanInitiateResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class UpscanServiceSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

	implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("testSessionId")))
	implicit val request: Request[AnyRef] = FakeRequest("GET", "http://localhost:9290/")
	val mockUpscanConnector: UpscanConnector = mock[UpscanConnectorImpl]
	val sessionId = "testSessionId"

	val upscanInitiateResponse: UpscanInitiateResponse = UpscanInitiateResponse(
		Reference("reference"),
		Call("post", "postTarget"),
		formFields = Map.empty[String, String]
	)


	class TestUpscanService extends UpscanService {
		override val applicationConfig: ApplicationConfig = mock[ApplicationConfig]
		override val upscanConnector: UpscanConnector = mockUpscanConnector
		override lazy val isSecure: Boolean = true
		override lazy val redirectUrlBase: String = "fakeUrlBase"
	}

	def testUpscanService = new TestUpscanService

  "getUpscanFormDataOds" must {
    "get form data from Upscan Connector with an initiate request" in {
      val callback = controllers.internal.routes.UpscanCallbackController.callbackOds(sessionId)
      val success = "fakeUrlBase" +controllers.routes.UpscanController.successODS("csop").url
      val failure = "fakeUrlBase" +controllers.routes.UpscanController.failure().url
      val expectedInitiateRequest = UpscanInitiateRequest(callback.absoluteURL(secure = true), success, failure)

      val initiateRequestCaptor = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier], any()))
        .thenReturn(Future.successful(upscanInitiateResponse))

      await(testUpscanService.getUpscanFormData(isCSV = false, "csop"))

      initiateRequestCaptor.getValue shouldBe expectedInitiateRequest
    }
  }

  "getUpscanFormDataCsv" must {
    "get form data from Upscan Connector with an initiate and uploadId" in {
      val uploadId = UploadId("TestUploadId")
			val upscanIds = UpscanIds(uploadId, "fileId", uploadStatus = NotStarted)

      val callback = controllers.internal.routes.UpscanCallbackController.callbackCsv(uploadId, sessionId)
      val success = "fakeUrlBase" +controllers.routes.UpscanController.successCSV(uploadId, "csop").url
      val failure = "fakeUrlBase" +controllers.routes.UpscanController.failure().url
      val expectedInitiateRequest = UpscanInitiateRequest(callback.absoluteURL(secure = true), success, failure)

			val initiateRequestCaptor = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier], any()))
        .thenReturn(Future.successful(upscanInitiateResponse))

      await(testUpscanService.getUpscanFormData(isCSV = true, scheme = "csop", Some(upscanIds)))

      initiateRequestCaptor.getValue shouldBe expectedInitiateRequest
    }
  }
}
