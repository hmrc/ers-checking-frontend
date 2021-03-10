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

package controllers

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import models.ERSFileProcessingException
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers.{any, anyString, refEq}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Messages, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents, Request, Result}
import play.api.test.FakeRequest
import play.api.{Application, i18n}
import services.{ProcessCsvService, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UploadControllerSpec extends TestKit(ActorSystem("UploadControllerTest")) with UnitSpec with ErsTestHelper with GuiceOneAppPerSuite{

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
  val mockProcessCsvService: ProcessCsvService = mock[ProcessCsvService]
  val uploadedSuccessfully: Option[UploadedSuccessfully] = Some(UploadedSuccessfully("testName", "testDownloadUrl", noOfRows = Some(1)))
  val callbackList: Option[UpscanCsvFilesCallbackList] = {
    Some(UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId.generate, uploadedSuccessfully.get))))
  }
  val mockStaxProcessor: StaxProcessor = mock[StaxProcessor]


  def buildFakeUploadControllerCsv(uploadRes: Boolean = true,
                                   processFile: Boolean = true,
                                   formatRes: Boolean = true,
                                   clearCacheResponse: Boolean = true,
                                   mockReadFileCsv: Boolean = true
                                  ): UploadController =
    new UploadController(mockAuthAction, mockProcessODSService, mockProcessCsvService, mcc, mockErsUtil, mockAppConfig) {

      override def clearErrorCache()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Boolean] =
        Future.successful(clearCacheResponse)

      override def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result = {
        ImATeapot("Test body")
      }

      val mockSource: Source[HttpResponse, NotUsed] = Source.fromIterator(() => List(HttpResponse(StatusCodes.OK)).toIterator)

      override private[controllers] def readFileCsv(downloadUrl: String): Source[HttpResponse, _] = if(mockReadFileCsv)
        mockSource
      else
        super.readFileCsv(downloadUrl)

      override private[controllers] def makeRequest(request: HttpRequest): Future[HttpResponse] = Future.successful(HttpResponse(StatusCodes.OK))

      val returnValue: List[Future[Either[Throwable, Boolean]]] = {
        if (processFile) {
          List(Future.successful(Right(uploadRes)))
        } else {
          List(Future.successful(Left(ERSFileProcessingException("", ""))))
        }
      }

      when(mockProcessCsvService.processFiles(any(), any(), any())(any(),any(),any())).thenReturn(returnValue)

      when(mockErsUtil.cache(refEq(ersUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any()))
        .thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))
      when(mockErsUtil.cache(refEq(ersUtil.FORMAT_ERROR_CACHE_PARAMS), any[Seq[String]])(any(), any(), any(), any()))
        .thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))
      when(mockErsUtil.cache(refEq(ersUtil.FORMAT_ERROR_EXTENDED_CACHE), any[Boolean])(any(), any(), any(), any()))
        .thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))

      when(mockErsUtil.shortLivedCache).thenReturn(mockShortLivedCache)
      when(mockShortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](any(), any())(any(),any(), any())).thenReturn(callbackList)

      mockAnyContentAction
    }

  "Calling UploadController.uploadCSVFile" should {
    implicit val fakeRequest: RequestWithOptionalEmpRef[AnyContent] = RequestWithOptionalEmpRef(Fixtures.buildFakeRequestWithSessionId("GET"), None)

    "give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
      UpscanCsvFilesCallbackList
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.uploadCSVFile(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }

    "give a redirect status to checkingSuccessPage if no formatting or structural errors" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest
        .uploadCSVFile(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckingServiceController.checkingSuccessPage().toString
    }

    "give a redirect status to checkingSuccessPage if formatting errors" in {
      val controllerUnderTest = buildFakeUploadControllerCsv(uploadRes = false)
      val result = controllerUnderTest
        .uploadCSVFile(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage(true).toString
    }

    "give a redirect status to globalErrorPage if clearErrorCache returns false" in {
      val controllerUnderTest = buildFakeUploadControllerCsv(clearCacheResponse = false)
      val result = controllerUnderTest
        .uploadCSVFile(Fixtures.getMockSchemeTypeString).apply(fakeRequest)
      status(result) shouldBe Status.IM_A_TEAPOT
      result.body.consumeData.utf8String shouldBe "Test body"

    }
  }

  "Calling finaliseRequestAndRedirect" should {
    "handle exception if failures are found" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.finaliseRequestAndRedirect(List(
        Future.successful(Left(ERSFileProcessingException("this is a problem", "help"))))
      )(RequestWithOptionalEmpRef(FakeRequest("GET", ""), None), hc)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckingServiceController.formatErrorsPage().toString
    }

    "send user to GlobalErrorPage if unexpected exception is found" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.finaliseRequestAndRedirect(List(
        Future.successful(Left(new Exception("taste the pain"))))
      )(RequestWithOptionalEmpRef(FakeRequest("GET", ""), None), hc)

      status(result) shouldBe Status.IM_A_TEAPOT
      result.body.consumeData.utf8String shouldBe "Test body"
    }
  }

  "Calling readFileCsv" should {
    "process the response" in {
      val controllerUnderTest = buildFakeUploadControllerCsv(mockReadFileCsv = false)
      val result: Future[Seq[HttpResponse]] = controllerUnderTest.readFileCsv("http://www.test.com").runWith(Sink.seq)

      val responses = Await.result(result, Duration.Inf)
      responses.length shouldBe 1
      responses.head shouldBe HttpResponse(StatusCodes.OK)

    }
  }

  "Calling handleException" should {
    "redirect to formatErrorPages if it's given a processing exception" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.handleException(ERSFileProcessingException("a", "b"))(FakeRequest("", ""), hc)

      val response = Await.result(result, Duration.Inf)
      status(response) shouldBe Status.SEE_OTHER
      response.header.headers("Location") shouldBe routes.CheckingServiceController.formatErrorsPage().toString
    }

    "redirect to globalErrorPage if it's given an UpstreamErrorResponse" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.handleException(UpstreamErrorResponse("a", Status.INTERNAL_SERVER_ERROR))(FakeRequest("", ""), hc)

      val response = Await.result(result, Duration.Inf)
      status(response) shouldBe Status.IM_A_TEAPOT
      result.body.consumeData.utf8String shouldBe "Test body"
    }

    "redirect to globalErrorPage if it's given an unexpected exception" in {
      val controllerUnderTest = buildFakeUploadControllerCsv()
      val result = controllerUnderTest.handleException(new Exception("this is a big whoops"))(FakeRequest("", ""), hc)

      val response = Await.result(result, Duration.Inf)
      status(response) shouldBe Status.IM_A_TEAPOT
      result.body.consumeData.utf8String shouldBe "Test body"
    }
  }

}
