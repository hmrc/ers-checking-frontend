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

import helpers.ErsTestHelper
import models.CSformMappings
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{DefaultMessagesControllerComponents, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CheckingServiceControllerTest extends UnitSpec with GuiceOneAppPerSuite with ErsTestHelper {

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  "start Page GET" should {

    def buildFakeCheckingServiceController(): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
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

    def buildFakeCheckingServiceController(): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
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

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
        when(mockErsUtil.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any()))
          .thenReturn(if (schemeRes) Future.successful(null) else Future.failed(new Exception))
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
      val controllerUnderTest = buildFakeCheckingServiceController()
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showSchemeTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckingServiceController.checkFileTypePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(await(controllerUnderTest.showSchemeTypeSelected(request)))shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, testMessages))
    }
  }


  "Check File Type Page GET" should {

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
      when(mockErsUtil.fetch[String](refEq(mockErsUtil.FILE_TYPE_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (fileTypeRes) Future.successful("csv") else Future.failed(new NoSuchElementException))
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

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
      when(mockErsUtil.cache(refEq(mockErsUtil.FILE_TYPE_CACHE), anyString())(any(), any(), any(), any()))
        .thenReturn(if (fileTypeRes) Future.successful(null) else Future.failed(new Exception))
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
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckCsvFilesController.selectCsvFilesPage().toString
    }

    "if no form errors with file type = ods and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "ods")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe routes.CheckingServiceController.checkODSFilePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = false)
      val schemeTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(await(controllerUnderTest.showCheckFileTypeSelected(request))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, testMessages))
    }
  }


  "Check file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
      when(mockErsUtil.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any()))
        .thenReturn(if (schemeRes) Future.successful(null) else Future.failed(new Exception))
      when(mockErsUtil.fetch[String](refEq(mockErsUtil.SCHEME_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))

			mockAnyContentAction
    }

    "gives a call to showCheckFilePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkODSFilePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check file page if fetch successful" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showCheckODSFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val res: Future[Result] = controllerUnderTest.showCheckODSFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      res.map { result =>
        result shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, testMessages))
      }    }

  }


  "Check CSV file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
      when(mockErsUtil.fetch[String](refEq(mockErsUtil.SCHEME_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))
			mockAnyContentAction
    }

    "gives a call to showCheckCSVFilePage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkCSVFilePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check csv file page if fetch successful" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showCheckCSVFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val res: Future[Result] = controllerUnderTest.showCheckCSVFilePage()(Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      (Fixtures.buildFakeRequestWithSessionId("GET"), hc, implicitly[Messages])
      res.map{ result =>
        result shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, testMessages))
      }}

  }

  "Checking success page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
																					 errorRes: Boolean = true,
																					 errorCount: String = "0"
																					): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {
        when(mockErsUtil.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any()))
          .thenReturn(if (schemeRes) Future.successful(null) else Future.failed(new Exception))

        when(mockErsUtil.fetch[String](refEq(mockErsUtil.SCHEME_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))

        when(mockErsUtil.fetch[String](refEq(mockErsUtil.SCHEME_ERROR_COUNT_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (errorRes) Future.successful(errorCount) else Future.failed(new Exception))

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

  "format errors page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
																					 fileTypeRes: String = "ods",
																					 errorRes: Boolean = true,
																					 errorCount: String = "0"
																					): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig) {

      when(mockErsUtil.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any(), any(), any()))
        .thenReturn(if (schemeRes) Future.successful(null) else Future.failed(new Exception))

      when(mockErsUtil.fetch[String](refEq(mockErsUtil.FILE_TYPE_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (schemeRes) Future.successful(fileTypeRes) else Future.failed(new Exception))

      when(mockErsUtil.fetch[String](refEq(mockErsUtil.FORMAT_ERROR_CACHE))(any(),any(),any(),any()))
        .thenReturn(if (errorRes) Future.successful(errorCount) else Future.failed(new Exception))

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
      val res: Future[Result] = controllerUnderTest.showFormatErrorsPage(Fixtures.buildFakeRequestWithSessionId("GET"), hc)
      res.map { result =>
        result shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, testMessages))
      }}
  }
}
