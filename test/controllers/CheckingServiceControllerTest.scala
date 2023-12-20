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

import helpers.ErsTestHelper
import models.CSformMappings
import models.upscan._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{Call, DefaultMessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import views.html._

import scala.concurrent.Future

class CheckingServiceControllerTest extends AnyWordSpecLike with Matchers with OptionValues
  with GuiceOneAppPerSuite with ErsTestHelper with Injecting with ScalaFutures {
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  val formatErrorsView: format_errors = inject[format_errors]
  val startView: start = inject[start]
  val schemeTypeView: scheme_type = inject[scheme_type]
  val checkFileTypeView: check_file_type = inject[check_file_type]
  val checkCsvFileView: check_csv_file = inject[check_csv_file]
  val checkFileView: check_file = inject[check_file]
  val checkingSuccessView: checking_success = inject[checking_success]
  val selectFileTypeView: select_csv_file_types = inject[select_csv_file_types]
  val globalErrorView: global_error = inject[global_error]
  val invalidErrorView: file_upload_problem = inject[file_upload_problem]
  val fileUploadErrorView: file_upload_error = inject[file_upload_error]
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  "start Page GET" should {

    def buildFakeCheckingServiceController(): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
        invalidErrorView, fileUploadErrorView, globalErrorView) {
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
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView, invalidErrorView, fileUploadErrorView, globalErrorView) {
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
      val result = controllerUnderTest.showSchemeTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

  }

  "Scheme Type Page POST" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView, invalidErrorView, fileUploadErrorView, globalErrorView) {
        when(mockSessionCacheRepo.cache[String](refEq(mockErsUtil.SCHEME_CACHE), any())(any(), any()))
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
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.showSchemeTypePage(form)(request))
    }

    "if no form errors with scheme type and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showSchemeTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckingServiceController.checkFileTypePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val schemeTypeData = Map("schemeType" -> "1")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(controllerUnderTest.showSchemeTypeSelected(request)) shouldBe
        contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
    }
  }


  "Check File Type Page GET" should {

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView, invalidErrorView, fileUploadErrorView, globalErrorView) {
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
      val result = controllerUnderTest.showCheckFileTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK if fetch fails then show check file type page with nothing selected" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = false)
      val form = CSformMappings.checkFileTypeForm.bind(Map("" -> ""))
      val result = controllerUnderTest.showCheckFileTypePage(form)(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[id=checkFileType-csv]").hasAttr("checked") shouldEqual false
      document.select("input[id=checkFileType-ods]").hasAttr("checked") shouldEqual false
    }

  }


  "Check File Type Page POST" should {

    def buildFakeCheckingServiceController(fileTypeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView, invalidErrorView, fileUploadErrorView, globalErrorView) {
        when(mockSessionCacheRepo.cache(refEq(mockErsUtil.FILE_TYPE_CACHE), anyString())(any(), any()))
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
      contentAsString(result) shouldBe contentAsString(controllerUnderTest.showCheckFileTypePage(form)(request))
    }

    "if no form errors with file type = csv and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckCsvFilesController.selectCsvFilesPage().toString
    }

    "if no form errors with file type = ods and save success" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val checkFileTypeData = Map("checkFileType" -> "ods")
      val form = CSformMappings.schemeTypeForm.bind(checkFileTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      val result = controllerUnderTest.showCheckFileTypeSelected(request)
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe routes.CheckingServiceController.checkODSFilePage().toString
    }

    "if no form errors with scheme type and save fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = false)
      val schemeTypeData = Map("checkFileType" -> "csv")
      val form = CSformMappings.schemeTypeForm.bind(schemeTypeData)
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withFormUrlEncodedBody(form.data.toSeq: _*)
      contentAsString(controllerUnderTest.showCheckFileTypeSelected(request)) shouldBe
        contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
    }
  }


  "Check file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
        invalidErrorView, fileUploadErrorView, globalErrorView) {

        when(mockSessionCacheRepo.cache[String](refEq(mockErsUtil.SCHEME_CACHE), any())(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(("", "")) else Future.failed(new Exception))

        when(mockSessionCacheRepo.fetch[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(Some("1")) else Future.successful(None))
        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))

        when(mockUpscanService.getUpscanFormData(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(UpscanInitiateResponse(Reference("ref"), Call("GET", "/"), Map.empty)))
        when(mockSessionCacheRepo.cache(any(), any())(any(), any()))
          .thenReturn(Future.successful(("", "")))
        when(mockSessionCacheRepo.cache[UploadStatus](any(), any())(any(), any()))
          .thenReturn(Future.successful(("", "")))

        mockAnyContentAction
      }

    "give a call to showCheckFilePage if user is authenticated" in {
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
        result shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
      }
    }

  }


  "Check CSV file page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
        invalidErrorView, fileUploadErrorView, globalErrorView) {
        when(mockSessionCacheRepo.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(("", "")) else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[UpscanCsvFilesList](refEq(mockErsUtil.CSV_FILES_UPLOAD))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(UpscanCsvFilesList(Seq(UpscanIds(UploadId("id"), "fileId", NotStarted)))) else Future.failed(new Exception))
        when(mockUpscanService.getUpscanFormData(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(UpscanInitiateResponse(Reference("ref"), Call("GET", "/"), Map.empty)))
        mockAnyContentAction
      }

    "give a call to showCheckCSVFilePage if user is authenticated" in {
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
      res.map { result =>
        result shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
      }
    }

  }

  "Checking success page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
                                           errorRes: Boolean = true,
                                           errorCount: String = "0"
                                          ): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
        invalidErrorView, fileUploadErrorView, globalErrorView) {
        when(mockSessionCacheRepo.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(("", "")) else Future.failed(new Exception))

        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful("1") else Future.failed(new Exception))

        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_ERROR_COUNT_CACHE))(any(), any()))
          .thenReturn(if (errorRes) Future.successful(errorCount) else Future.failed(new Exception))

        mockAnyContentAction
      }

    "give a call to showCheckingSuccessPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkingSuccessPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check success page" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.showCheckingSuccessPage()(Fixtures.buildFakeRequestWithSessionId("GET"), implicitly[Messages])
      status(result) shouldBe Status.OK
    }

  }

  "format errors page GET" should {

    def buildFakeCheckingServiceController(schemeRes: Boolean = true,
                                           fileTypeRes: String = "ods",
                                           errorRes: Boolean = true,
                                           errorCount: String = "0"
                                          ): CheckingServiceController =
      new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
        formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
        invalidErrorView, fileUploadErrorView, globalErrorView) {

        when(mockSessionCacheRepo.cache(refEq(mockErsUtil.SCHEME_CACHE), anyString())(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(("", "")) else Future.failed(new Exception))
        when(mockErsUtil.getSchemeName(any())).thenReturn(("a", "b"))

        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.FILE_TYPE_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful(fileTypeRes) else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
          .thenReturn(if (schemeRes) Future.successful("csop") else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[Boolean](refEq(mockErsUtil.FORMAT_ERROR_EXTENDED_CACHE))(any(), any()))
          .thenReturn(if (errorRes) Future.successful(false) else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.FORMAT_ERROR_CACHE))(any(), any()))
          .thenReturn(if (errorRes) Future.successful(errorCount) else Future.failed(new Exception))
        when(mockSessionCacheRepo.fetchAndGetEntry[Seq[String]](refEq(mockErsUtil.FORMAT_ERROR_CACHE_PARAMS))(any(), any()))
          .thenReturn(if (errorRes) Future.successful(Seq(errorCount)) else Future.failed(new Exception))

        mockAnyContentAction
      }

    "gives a call to showFormatErrorsPage if user is authenticated" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "give a status OK and shows check errors page if fetch successful for scheme type and error count" in {
      val controllerUnderTest = buildFakeCheckingServiceController(fileTypeRes = "csv")
      val result = controllerUnderTest.showFormatErrorsPage()(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }

    "direct to ers errors page if fetch fails" in {
      val controllerUnderTest = buildFakeCheckingServiceController(schemeRes = false)
      val res: Future[Result] = controllerUnderTest.showFormatErrorsPage()(Fixtures.buildFakeRequestWithSessionId("GET"))
      res.map { result =>
        result shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
      }
    }

    "gives a call to showFileUploadErrorPage if user is authenticated and clicks browser back button" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.fileUploadError().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "direct to checkingInvalidFilePage if user uploads invalid file" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.checkingInvalidFilePage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "verify page title is present" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      assert(contentAsString(result).contains(Messages("ers_format_errors.page_title")))
    }

    "verify sub title is present for the you can section" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      assert(contentAsString(result).contains(Messages("ers_error_report.view_as_html2")))
    }

    "verify hyperlink1 is present under the you can section" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      assert(contentAsString(result).contains(Messages("ers.file_upload_error.instructions1.hyperlink")))
    }

    "verify hyperlink2 is present under the you can section" in {
      val controllerUnderTest = buildFakeCheckingServiceController()
      val result = controllerUnderTest.formatErrorsPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      assert(contentAsString(result).contains(Messages("ers.file_upload_error.instructions2.hyperlink")))
    }
  }
}
