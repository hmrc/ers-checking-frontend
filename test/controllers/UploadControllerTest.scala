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
import akka.http.scaladsl.model.{HttpResponse, StatusCode, StatusCodes}
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import akka.util.ByteString
import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import models.ERSFileProcessingException
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.{Messages, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.{Application, i18n}
import services.{ProcessCsvService, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class UploadControllerTest extends TestKit(ActorSystem("UploadControllerTest")) with UnitSpec with ErsTestHelper with GuiceOneAppPerSuite {

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

  def buildFakeUploadControllerOds(uploadRes: Boolean = true,
																	 proccessFile: Boolean = true,
																	 formatRes: Boolean = true,
																	 clearCacheResponse: Boolean = true
																	): UploadController =
		new UploadController(mockAuthAction, mockProcessODSService, mockProcessCsvService, mcc, mockErsUtil, mockAppConfig) {

		override def clearErrorCache()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Boolean] =
			Future.successful(clearCacheResponse)

		override private[controllers] def readFileOds(downloadUrl: String): StaxProcessor = mockStaxProcessor

		when(mockProcessODSService.performODSUpload(any(), any())(any(),any(),any(),any()))
			.thenReturn(if (proccessFile) Future.successful(Success(uploadRes)) else Future.successful(Failure(ERSFileProcessingException("", ""))))

		when(mockProcessCsvService.processCsvUpload(any(), any(), any(), any())(any(),any(),any()))
			.thenReturn(if (proccessFile) Future.successful(Success(uploadRes)) else Future.successful(Failure(ERSFileProcessingException("", ""))))

		when(mockErsUtil.cache(refEq(mockErsUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any()))
			.thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))

		when(mockErsUtil.shortLivedCache).thenReturn(mockShortLivedCache)
		when(mockShortLivedCache.fetchAndGetEntry[UploadedSuccessfully](any(), any())(any(),any(), any())).thenReturn(uploadedSuccessfully)

		mockAnyContentAction
	}

	def buildFakeUploadControllerCsv(uploadRes: Boolean = true,
																	 proccessFile: Boolean = true,
																	 formatRes: Boolean = true,
																	 clearCacheResponse: Boolean = true
																	): UploadController =
		new UploadController(mockAuthAction, mockProcessODSService, mockProcessCsvService, mcc, mockErsUtil, mockAppConfig) {

		override def clearErrorCache()(implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Boolean] =
			Future.successful(clearCacheResponse)

			val mockSource: Source[HttpResponse, NotUsed] = Source.fromIterator(() => List(HttpResponse(StatusCodes.OK)).toIterator)

		override private[controllers] def readFileCsv(downloadUrl: String): Source[HttpResponse, _] = mockSource
		val returnValue: Future[Try[Boolean]] = {
			if (proccessFile) {
				Future.successful(Success(uploadRes))
			} else {
				Future.successful(Failure(ERSFileProcessingException("", "")))
			}
		}

		when(mockProcessCsvService.processCsvUpload(any(), any(), any(), any())(any(),any(),any())).thenReturn(returnValue)

		when(mockErsUtil.cache(refEq(ersUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any()))
			.thenReturn(if (formatRes) Future.successful(null) else Future.failed(new Exception))

		when(mockErsUtil.shortLivedCache).thenReturn(mockShortLivedCache)
		when(mockShortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](any(), any())(any(),any(), any())).thenReturn(callbackList)

		mockAnyContentAction
	}

	"Calling UploadController.uploadODSFile" should {

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest: FakeRequest[AnyContent] = Fixtures.buildFakeRequestWithSessionId("GET")
			val controllerUnderTest = buildFakeUploadControllerOds()

			val result = controllerUnderTest.uploadODSFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status to checkingSuccessPage if no formating or structural errors" in {
			val controllerUnderTest = buildFakeUploadControllerOds()
			val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.CheckingServiceController.checkingSuccessPage().toString
		}

		"give a redirect status to checkingSuccessPage if formating errors" in {
			val controllerUnderTest = buildFakeUploadControllerOds(uploadRes = false)
			val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage(false).toString
		}

	}

	"Calling UploadController.uploadCSVFile" should {

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest: FakeRequest[AnyContent] = Fixtures.buildFakeRequestWithSessionId("GET")
			UpscanCsvFilesCallbackList
			val controllerUnderTest = buildFakeUploadControllerCsv()
			val result = controllerUnderTest.uploadCSVFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status to checkingSuccessPage if no formating or structural errors" in {
			val controllerUnderTest = buildFakeUploadControllerCsv()
			val result = controllerUnderTest.showUploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.CheckingServiceController.checkingSuccessPage().toString
		}

		"give a redirect status to checkingSuccessPage if formating errors" in {
			val controllerUnderTest = buildFakeUploadControllerCsv(uploadRes = false)
			val result = controllerUnderTest.showUploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage(true).toString
		}
	}
}
