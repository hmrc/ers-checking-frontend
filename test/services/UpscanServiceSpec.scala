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

import connectors.UpscanConnector
import helpers.ErsTestHelper
import models.upscan._
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UpscanServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with ErsTestHelper {

  override implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "http://localhost:9290/")
  val mockUpscanConnector: UpscanConnector = mock[UpscanConnector]
  val sessionId = "testSessionId"

  val upscanInitiateResponse: UpscanInitiateResponse = UpscanInitiateResponse(
    Reference("reference"),
    Call("post", "postTarget"),
    formFields = Map.empty[String, String]
  )

  class TestUpscanService extends UpscanService(mockUpscanConnector, mockAppConfig) {
    override lazy val isSecure: Boolean = true
    override lazy val redirectUrlBase: String = "fakeUrlBase"
  }

  "getUpscanFormDataOds" must {
    "get form data from Upscan Connector with an initiate request" in new TestUpscanService {
      val callback: Call = controllers.internal.routes.UpscanCallbackController.callbackOds(sessionId)
      val success: String = "fakeUrlBase" + controllers.routes.UpscanController.successODS("csop").url
      val failure: String = "fakeUrlBase" + controllers.routes.UpscanController.failure().url
      val expectedInitiateRequest: UpscanInitiateRequest = UpscanInitiateRequest(callback.absoluteURL(secure = true), success, failure, Some(1), Some(209715200))
      val initiateRequestCaptor: ArgumentCaptor[UpscanInitiateRequest] = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanInitiateResponse))

      Await.ready(getUpscanFormData(isCSV = false, "csop"), Duration.Inf)
      initiateRequestCaptor.getValue shouldBe expectedInitiateRequest
    }
  }

  "getUpscanFormDataCsv" must {
    "get form data from Upscan Connector with an initiate and uploadId" in new TestUpscanService {
      val uploadId: UploadId = UploadId("TestUploadId")
      val upscanIds: UpscanIds = UpscanIds(uploadId, "fileId", uploadStatus = NotStarted)

      val callback: Call = controllers.internal.routes.UpscanCallbackController.callbackCsv(uploadId, sessionId)
      val success: String = "fakeUrlBase" + controllers.routes.UpscanController.successCSV(uploadId, "csop").url
      val failure: String = "fakeUrlBase" + controllers.routes.UpscanController.failure().url
      val expectedInitiateRequest: UpscanInitiateRequest = UpscanInitiateRequest(callback.absoluteURL(secure = true), success, failure, Some(1), Some(209715200))
      val initiateRequestCaptor: ArgumentCaptor[UpscanInitiateRequest] = ArgumentCaptor.forClass(classOf[UpscanInitiateRequest])

      when(mockUpscanConnector.getUpscanFormData(initiateRequestCaptor.capture())(any[HeaderCarrier]))
        .thenReturn(Future.successful(upscanInitiateResponse))

      Await.ready(getUpscanFormData(isCSV = true, scheme = "csop", Some(upscanIds)), Duration.Inf)
      initiateRequestCaptor.getValue shouldBe expectedInitiateRequest
    }
  }
}
