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

package controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import helpers.ErsTestHelper
import models.ERSFileProcessingException
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Messages, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents, Request}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Injecting}
import play.api.{Application, i18n}
import services.{ProcessCsvService, ProcessODSService, StaxProcessor}
import views.html.global_error

import scala.concurrent.Future
import scala.util.{Failure, Success}

class UploadControllerTest extends TestKit(ActorSystem("UploadControllerTest")) with AnyWordSpecLike with Matchers
  with OptionValues with ErsTestHelper with GuiceOneAppPerSuite with Injecting with ScalaFutures {
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  val config: Map[String, Any] = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false,
    "mongodb.timeToLiveInSeconds" -> 3600)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
  val mockProcessCsvService: ProcessCsvService = mock[ProcessCsvService]
  val uploadedSuccessfully: Option[UploadedSuccessfully] = Some(UploadedSuccessfully("testName", "testDownloadUrl", noOfRows = Some(1)))
  val callbackList: Option[UpscanCsvFilesCallbackList] = {
    Some(UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId.generate, uploadedSuccessfully.get))))
  }
  val mockStaxProcessor: StaxProcessor = mock[StaxProcessor]
  val globalErrorView: global_error = inject[global_error]

  def buildFakeUploadControllerOds(uploadRes: Boolean = true,
                                   processFile: Boolean = true,
                                   formatRes: Boolean = true,
                                   clearCacheResponse: Boolean = true
                                  ): UploadController =
    new UploadController(mockAuthAction, mockProcessODSService, mockProcessCsvService, mockSessionCacheRepo, mcc, globalErrorView) {

      override def clearErrorCache()(implicit request: Request[_]): Future[Boolean] =
        Future.successful(clearCacheResponse)

      override private[controllers] def readFileOds(downloadUrl: String): StaxProcessor = mockStaxProcessor

      when(mockProcessODSService.performODSUpload(any(), any())(any(), any(), any(), any()))
        .thenReturn(if (processFile) Future.successful(Success(uploadRes)) else Future.successful(Failure(ERSFileProcessingException("", ""))))

      when(mockSessionCacheRepo.cache(refEq(mockErsUtil.FORMAT_ERROR_CACHE), anyString())(any(), any()))
        .thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))

      when(mockSessionCacheRepo.fetch[UploadedSuccessfully](any())(any(), any()))
        .thenReturn(Future.successful(uploadedSuccessfully))

      mockAnyContentAction
    }

  "Calling UploadController.uploadODSFile" should {

    "give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
      implicit val fakeRequest: FakeRequest[AnyContent] = Fixtures.buildFakeRequestWithSessionId("GET")
      val controllerUnderTest = buildFakeUploadControllerOds()

      val result = controllerUnderTest.uploadODSFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a redirect status to checkingSuccessPage if no formatting or structural errors" in {
      val controllerUnderTest = buildFakeUploadControllerOds()
      val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckingServiceController.checkingSuccessPage().toString
    }

    "give a redirect status to checkingSuccessPage if formatting errors" in {
      val controllerUnderTest = buildFakeUploadControllerOds(uploadRes = false)
      val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage(false).toString
    }

    "send the user to the global error page if the error cache fails to clear" in {
      val controllerUnderTest = buildFakeUploadControllerOds(clearCacheResponse = false)
      val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }
}
