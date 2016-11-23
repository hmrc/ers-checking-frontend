/*
 * Copyright 2016 HM Revenue & Customs
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

import models.ERSFileProcessingException
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import services.{CsvFileProcessor, ProcessODSService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import utils.CacheUtil
import scala.concurrent.Future

class UploadControllerTest extends UnitSpec with OneAppPerSuite with MockitoSugar {

  def buildFakeUploadController(uploadRes: Boolean = true, proccessFile: Boolean = true, formatRes: Boolean = true) = new UploadController {

		val mockProcessODSService: ProcessODSService = mock[ProcessODSService]
		val mockCsvFileProcessor: CsvFileProcessor = mock[CsvFileProcessor]
		override val csvFileProcessor:CsvFileProcessor = mockCsvFileProcessor
		override val processODSService: ProcessODSService = mockProcessODSService
		when(
			mockProcessODSService.performODSUpload()(any(),any(),any(),any(),any())
		).thenReturn(
				proccessFile match {
				case true => Future.successful(uploadRes)
				case _ => Future.failed(new ERSFileProcessingException("",""))
			}
		)
		when(
			csvFileProcessor.processCsvUpload(any())(any(),any(),any())
		).thenReturn(
				proccessFile match {
				case true => Future.successful(uploadRes)
				case _ => Future.failed(new ERSFileProcessingException("",""))
			}
		)

		val mockCacheUtil: CacheUtil = mock[CacheUtil]
		override val cacheUtil: CacheUtil = mockCacheUtil
		when(
			mockCacheUtil.cache(refEq(CacheUtil.FORMAT_ERROR_CACHE), anyString())(any(), any(), any(), any())
		).thenReturn(
			formatRes match {
				case true => Future.successful(null)
				case _ => Future.failed(new Exception)
			}
		)
  }

	"Calling UploadController.uploadODSFile" should {

		"give a redirect status (to company authentication frontend) if user is not authenticated" in {
			val controllerUnderTest = buildFakeUploadController()
			val result = controllerUnderTest.uploadODSFile(Fixtures.getMockSchemeTypeString).apply(FakeRequest("GET", ""))
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
			val controllerUnderTest = buildFakeUploadController()
			val result = controllerUnderTest.uploadODSFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
			//result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingSuccessPage.toString()
		}

		"give a redirect status to checkingSuccessPage if no formating or structural errors" in {
			val controllerUnderTest = buildFakeUploadController()
			implicit val hc = new HeaderCarrier
			val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingSuccessPage.toString()
		}

		"give a redirect status to checkingSuccessPage if formating errors" in {
			val controllerUnderTest = buildFakeUploadController(uploadRes = false)
			implicit val hc = new HeaderCarrier
			val result = controllerUnderTest.showuploadODSFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers.get("Location").get shouldBe routes.HtmlReportController.htmlErrorReportPage.toString()
		}

	}

	"Calling UploadController.uploadCSVFile" should {

		"give a redirect status (to company authentication frontend) if user is not authenticated" in {
			val controllerUnderTest = buildFakeUploadController()
			val result = controllerUnderTest.uploadCSVFile(Fixtures.getMockSchemeTypeString).apply(FakeRequest("GET", ""))
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status and show checkingSuccessPage if authenticated and no validation errors" in {
			implicit val fakeRequest = Fixtures.buildFakeRequestWithSessionId("GET")
			val controllerUnderTest = buildFakeUploadController()
			val result = controllerUnderTest.uploadCSVFile(Fixtures.getMockSchemeTypeString)(fakeRequest)
			status(result) shouldBe Status.SEE_OTHER
		}

		"give a redirect status to checkingSuccessPage if no formating or structural errors" in {
			val controllerUnderTest = buildFakeUploadController()
			implicit val hc = new HeaderCarrier
			val result = controllerUnderTest.showuploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingSuccessPage.toString()
		}

		"give a redirect status to checkingSuccessPage if formating errors" in {
			val controllerUnderTest = buildFakeUploadController(uploadRes = false)
			implicit val hc = new HeaderCarrier
			val result = controllerUnderTest.showuploadCSVFile(Fixtures.getMockSchemeTypeString)(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
			status(result) shouldBe Status.SEE_OTHER
			result.header.headers.get("Location").get shouldBe routes.HtmlReportController.htmlErrorReportPage.toString()
		}

	}

}
