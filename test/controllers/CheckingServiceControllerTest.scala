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
import models.{CS_schemeType, CSformMappings}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.http.Status
import play.api.libs.json.JsString
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import utils.CacheUtil
import play.api.mvc.Request
import services.UpscanService

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap


class CheckingServiceControllerTest extends UnitSpec with OneAppPerSuite with MockitoSugar with WithMockedAuthActions {

  implicit val hc: HeaderCarrier = new HeaderCarrier
  implicit lazy val messages: Messages = Messages(Lang("en"), app.injector.instanceOf[MessagesApi])
  implicit val request: Request[_] = FakeRequest()
  val mockAuthAction : AuthAction = mock[AuthAction]
	val mockUpscanService: UpscanService = mock[UpscanService]

  "start Page GET" should {

    def buildFakeCheckingServiceController(): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showStartPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.startPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows start page" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showStartPage()(Fixtures.buildFakeRequestWithSessionId("GET"), implicitly[Messages])
      status(result) shouldBe Status.OK
    }

  }

  "Scheme Type Page GET" should {

    def buildFakeCheckingServiceController(): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showSchemeTypePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.schemeTypePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows scheme type page" in {

      val controllerUnderTest = buildFakeCheckingServiceController()

      val form = CSformMappings.schemeTypeForm.bind(Map("" -> ""))
      val result = controllerUnderTest.showSchemeTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }

  }

  "Scheme Type Page POST" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      when(
        mockCacheUtil.cache(refEq(CacheUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showSchemeTypeSelected if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.schemeTypeSelected().apply(Fixtures.buildFakeRequestWithSessionId("POST"))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "give a bad request status and stay on the same page if form errors" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val schemeTypeData = Map("" -> "")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showSchemeTypeSelected(request)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(await(result)) shouldBe contentAsString(controllerUnderTest.showSchemeTypePage(form)(request, hc))
    }

    "if no form errors with scheme type and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = true)
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showSchemeTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkFileTypePage.toString()
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(await(controllerUnderTest.showSchemeTypeSelected(request))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }

//    "give a redirect Status and shows the check file page" in {
//      val controllerUnderTest = buildFakeCheckingServiceController()
//      val result = controllerUnderTest.showSchemeTypeSelected(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"))
//      status(result) shouldBe Status.SEE_OTHER
//    }

  }


  "Check File Type Page GET" should {

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.FILE_TYPE_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        fileTypeRes match {
          case true => Future.successful("csv")
          case _ => Future.failed(new NoSuchElementException)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckFileTypePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK if fetch successful and shows check file type page" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val form = CSformMappings.checkFileTypeForm.bind(Map("" -> ""))
      val result = controllerUnderTest.showCheckFileTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
    }

    "give a status OK if fetch fails then show check file type page with nothing selected" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = false)
      val form = CSformMappings.checkFileTypeForm.bind(Map("" -> ""))
      val result = controllerUnderTest.showCheckFileTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=checkFileType-csv]").hasAttr("checked") shouldEqual false
      document.select("input[id=checkFileType-ods]").hasAttr("checked") shouldEqual false
    }

  }


  "Check File Type Page POST" should {

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      when(
        mockCacheUtil.cache(refEq(CacheUtil.FILE_TYPE_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        fileTypeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckFileTypeSelected if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkFileTypeSelected().apply(Fixtures.buildFakeRequestWithSessionId("POST"))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "give a bad request status and stay on the same page if form errors" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val fileTypeData = Map("" -> "")
      val form = CSformMappings.checkFileTypeForm.bind(fileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(await(result)) shouldBe contentAsString(controllerUnderTest.showCheckFileTypePage(form)(request, hc))
    }

    "if no form errors with file type = csv and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = true)
      val checkFileTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location").get shouldBe routes.CheckCsvFilesController.selectCsvFilesPage.toString()
    }

    "if no form errors with file type = ods and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = true)
      val checkFileTypeData = Map("checkFileType" -> "ods")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkODSFilePage.toString()
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = false)
      val schemeTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(await(controllerUnderTest.showCheckFileTypeSelected(request))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }

  }



  "Check file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      val scheme: String = "1"
      when(
        mockCacheUtil.cache(refEq(CacheUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful("1")
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckFilePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkODSFilePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check file page if fetch successful" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = true)
      val result = controllerUnderTest.showCheckODSFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      contentAsString(await( controllerUnderTest.showCheckODSFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages]))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }

  }


  "Check CSV file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      val scheme: String = "1"
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful("1")
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckCSVFilePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkCSVFilePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check csv file page if fetch successful" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = true)
      val result = controllerUnderTest.showCheckCSVFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      contentAsString(await( controllerUnderTest.showCheckCSVFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages]))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }

  }




  "Checking success page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
																					 errorRes: Boolean = true,
																					 errorCount: String = "0"
																					): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      val scheme: String = "1"
      when(
        mockCacheUtil.cache(refEq(CacheUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful("1")
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_ERROR_COUNT_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        errorRes match {
          case true => Future.successful(errorCount)
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckingSuccessPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkingSuccessPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check success page" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showCheckingSuccessPage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.OK
    }

  }

  "checking errors page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
																					 fileTypeRes: String = "ods",
																					 errorRes: Boolean = true,
																					 errorCount: String = "0"
																					): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      val scheme: String = "1"
      when(
        mockCacheUtil.cache(refEq(CacheUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.FILE_TYPE_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(fileTypeRes)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_ERROR_COUNT_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        errorRes match {
          case true => Future.successful(errorCount)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetchAll()(any(),any(),any())
      ).thenReturn(
        Future.successful(CacheMap("test", Map(CacheUtil.SCHEME_CACHE -> JsString("test"))))
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showCheckingErrorsPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkingErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

//    "give a status OK and shows check errors page if fetch successful and scheme type is csv" in {
//      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = "csv")
//      val result = controllerUnderTest.showCheckingErrorsPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
//      status(result) shouldBe Status.OK
////      result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingErrorsPage.toString()
//    }

//    "give a status OK and shows check errors page if fetch successful and scheme type is ods" in {
//      val controllerUnderTest = buildFakeCheckingServiceController()
//      val result = controllerUnderTest.showCheckingErrorsPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
//      status(result) shouldBe Status.OK
//      //result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingErrorsPage.toString()
//    }

//    "direct to ers errors page if fetch fails" in {
//      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
//      contentAsString(await(controllerUnderTest.showCheckingErrorsPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
//    }

  }


  "format errors page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
																					 fileTypeRes: String = "ods",
																					 errorRes: Boolean = true,
																					 errorCount: String = "0"
																					): CheckingServiceController = new CheckingServiceController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      val scheme: String = "1"
      when(
        mockCacheUtil.cache(refEq(CacheUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(null)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.FILE_TYPE_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        schemeRes match {
          case true => Future.successful(fileTypeRes)
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.FORMAT_ERROR_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        errorRes match {
          case true => Future.successful(errorCount)
          case _ => Future.failed(new Exception)
        }
      )
      override val authAction: AuthAction = mockAuthAction
			override val upscanService: UpscanService = mockUpscanService
			mockAnyContentAction
    }

    "gives a call to showFormatErrorsPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check errors page if fetch successful for scheme type and error count" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = "csv")
      val result = controllerUnderTest.showFormatErrorsPage(Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      contentAsString(await(controllerUnderTest.showFormatErrorsPage(Fixtures.buildFakeRequestWithSessionId("GET"), hc))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }
  }
}
