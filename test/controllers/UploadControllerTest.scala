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

package controllers

import controllers.auth.AuthAction
import helpers.WithMockedAuthActions
import models.ERSFileProcessingException
import models.upscan.{NotStarted, UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Logger}
import play.api.http.Status
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import services.{CsvFileProcessor, ProcessODSService, StaxProcessor}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import uk.gov.hmrc.play.test.UnitSpec
import utils.CacheUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class UploadControllerTest extends UnitSpec with MockitoSugar with GuiceOneAppPerSuite with I18nSupport with WithMockedAuthActions {

	val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

	implicit val hc: HeaderCarrier = new HeaderCarrier

	override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build
	def injector: Injector = app.injector
	implicit val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]
	val mockAuthAction : AuthAction = mock[AuthAction]
	val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
	val mockCacheUtil: CacheUtil = mock[CacheUtil]
	val mockCsvFileProcessor: CsvFileProcessor = mock[CsvFileProcessor]
	val mockShortLivedCache: ShortLivedCache = mock[ShortLivedCache]
	val uploadedSuccessfully: Option[UploadedSuccessfully] = Some(UploadedSuccessfully("testName", "testDownloadUrl", noOfRows = Some(1)))
	val callbackList: Option[UpscanCsvFilesCallbackList] = {
		Some(UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId.generate, uploadedSuccessfully.get))))
	}
	val mockStaxProcessor: StaxProcessor = mock[StaxProcessor]
	val mockIterator: Iterator[String] = mock[Iterator[String]]

  def buildFakeUploadControllerOds(uploadRes: Boolean = true,
																proccessFile: Boolean = true,
																formatRes: Boolean = true
															 ): UploadController = new UploadController {

		override val csvFileProcessor:CsvFileProcessor = mockCsvFileProcessor
		override val processODSService: ProcessODSService = mockProcessODSService
		override val authAction: AuthAction = mockAuthAction
		override val cacheUtil: CacheUtil = mockCacheUtil

		override private[controllers] def readFileOds(downloadUrl: String): StaxProcessor = mockStaxProcessor

		when(mockProcessODSService.performODSUpload(any(), any())(any(),any(),any(),any())).thenReturn(
				if (proccessFile) {
					Future.successful(Success(uploadRes))
				} else {
					Future.successful(Failure(new ERSFileProcessingException("", "")))
				}
		)
		when(csvFileProcessor.processCsvUpload(any(), any(), any())(any(),any(),any())).thenReturn(
				if (proccessFile) {
					Future.successful(Success(uploadRes))
				} else {
					Future.successful(Failure(new ERSFileProcessingException("", "")))
				}
		)

		when(mockCacheUtil.cache(refEq(CacheUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any())).thenReturn(
			if (formatRes) {
				Future.successful(null)
			} else {
				Future.failed(new Exception)
			}
		)

		when(mockCacheUtil.shortLivedCache).thenReturn(mockShortLivedCache)
		when(mockShortLivedCache.fetchAndGetEntry[UploadedSuccessfully](any(), any())(any(),any(), any())).thenReturn(uploadedSuccessfully)

		mockAnyContentAction
	}

	def buildFakeUploadControllerCsv(uploadRes: Boolean = true,
																	 proccessFile: Boolean = true,
																	 formatRes: Boolean = true
																	): UploadController = new UploadController {

		override val csvFileProcessor:CsvFileProcessor = mockCsvFileProcessor
		override val processODSService: ProcessODSService = mockProcessODSService
		override val authAction: AuthAction = mockAuthAction
		override val cacheUtil: CacheUtil = mockCacheUtil

		override private[controllers] def readFileCsv(downloadUrl: String)
																								 (implicit ec: ExecutionContext): Future[Iterator[String]] = Future.successful(mockIterator)
		val returnValue: Future[Try[Boolean]] = {
			if (proccessFile) {
				Future.successful(Success(uploadRes))
			} else {
				Future.successful(Failure(ERSFileProcessingException("", "")))
			}
		}

		when(csvFileProcessor.processCsvUpload(any(), any(), any())(any(),any(),any())).thenReturn(returnValue)

		when(mockCacheUtil.cache(refEq(CacheUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any())).thenReturn(
			if (formatRes) {
				Future.successful(null)
			} else {
				Future.failed(new Exception)
			}
		)

		when(mockCacheUtil.shortLivedCache).thenReturn(mockShortLivedCache)
		when(mockShortLivedCache.fetchAndGetEntry[UpscanCsvFilesCallbackList](any(), any())(any(),any(), any())).thenReturn(callbackList)

		mockAnyContentAction
	}

	"Calling UploadController.uploadODSFile" should {

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
			val controllerUnderTest = buildFakeUploadControllerOds()

			val result = controllerUnderTest.uploadODSFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
			//result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingSuccessPage.toString()
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
			result.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage().toString
		}

	}

	"Calling UploadController.uploadCSVFile" should {

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
			UpscanCsvFilesCallbackList
			val controllerUnderTest = buildFakeUploadControllerCsv()
			val result = controllerUnderTest.uploadCSVFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status to checkingSuccessPage if no formating or structural errors" in {
			val controllerUnderTest = buildFakeUploadControllerCsv()
			val result = controllerUnderTest.showuploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.CheckingServiceController.checkingSuccessPage().toString
		}

		"give a redirect status to checkingSuccessPage if formating errors" in {
			val controllerUnderTest = buildFakeUploadControllerCsv(uploadRes = false)
			val result = controllerUnderTest.showuploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc, implicitly[Messages])
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers("Location") shouldBe routes.HtmlReportController.htmlErrorReportPage().toString
		}

	}

}
