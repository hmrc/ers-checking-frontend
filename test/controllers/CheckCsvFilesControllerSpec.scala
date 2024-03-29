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

import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import models.CsvFiles
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesList, UpscanIds}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.data.Form
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.HttpResponse
import views.html._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CheckCsvFilesControllerSpec extends AnyWordSpecLike with Matchers with OptionValues with GuiceOneAppPerSuite
  with ErsTestHelper with Injecting with ScalaFutures {

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
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

  def buildFakeCheckingServiceController(): CheckingServiceController =
    new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionCacheRepo, mcc,
      formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView,
      invalidErrorView, fileUploadErrorView, globalErrorView) {
      mockAnyContentAction
    }

  "selectCsvFilesPage" should {

    "call showCheckCsvFilesPage if user is authenticated" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def showCheckCsvFilesPage()(
          implicit request: RequestWithOptionalEmpRef[AnyContent]): Future[Result] = Future.successful(Ok("Authenticated"))
        mockAnyContentAction
      }
      val result = controllerUnderTest.selectCsvFilesPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe "Authenticated"
    }
  }

  "showCheckCsvFilesPage" should {
    "go to global error page if an exception was thrown" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def getGlobalErrorPage(implicit request: Request[AnyRef], messages: Messages): Result =
          InternalServerError("this is very bad")
        mockAnyContentAction
      }
      when(mockSessionCacheRepo.delete(refEq(mockErsUtil.CSV_FILES_UPLOAD))(any()))
        .thenReturn(Future.failed(new RuntimeException("this failed tbh")))

      val result = controllerUnderTest.showCheckCsvFilesPage()(Fixtures.buildEmpRefRequestWithSessionId("GET"))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe "this is very bad"
    }

    "go to the select_csv_file_types page when successful" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def getGlobalErrorPage(implicit request: Request[AnyRef], messages: Messages): Result =
          InternalServerError("this is very bad")
        mockAnyContentAction
      }
      when(mockSessionCacheRepo.delete(refEq(mockErsUtil.CSV_FILES_UPLOAD))(any()))
        .thenReturn(Future.successful(HttpResponse(Status.OK, "we uploadin")))
      when(mockSessionCacheRepo.fetchAndGetEntry[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any()))
        .thenReturn(Future.successful("a string"))
      when(mockErsUtil.getCsvFilesList(any())).thenReturn(Seq(CsvFiles("a file")))
      when(mockErsUtil.getPageElement(any(), any(), any())).thenReturn("this is okay!")

      val result = controllerUnderTest.showCheckCsvFilesPage()(Fixtures.buildEmpRefRequestWithSessionId("GET"))

      status(result) shouldBe Status.OK
      assert(contentAsString(result).contains("this is okay!"))
    }

  }

  "checkCsvFilesPageSelected" should {

    "call validateCsvFilesPageSelected if user is authenticated" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def validateCsvFilesPageSelected()(
          implicit request: RequestWithOptionalEmpRef[AnyContent]): Future[Result] = Future.successful(Ok("here we go!"))
        mockAnyContentAction
      }
      val result = controllerUnderTest.checkCsvFilesPageSelected().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe "here we go!"
    }
  }

  "validateCsvFilesPageSelected" should {

    "call reloadWithError if form has errors" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        mockAnyContentAction
      }
      val request = FakeRequest("GET", "").withFormUrlEncodedBody(("files", "file"), ("fileId", "asdasd£$aaa"))

      val result = controllerUnderTest.validateCsvFilesPageSelected()(RequestWithOptionalEmpRef(request, None))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe "this is a reload"
    }

    "call reloadWithError if no file selected" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        mockAnyContentAction
      }
      val request = FakeRequest("GET", "")

      val result = controllerUnderTest.validateCsvFilesPageSelected()(RequestWithOptionalEmpRef(request, None))
      status(result) shouldBe Status.SEE_OTHER
    }

    "call reloadWithError if form does not have errors" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def performCsvFilesPageSelected(formData: Seq[CsvFiles])(
          implicit request: Request[AnyRef]): Future[Result] = Future.successful(Ok("form good"))
        mockAnyContentAction
      }
      val request = FakeRequest("GET", "").withFormUrlEncodedBody(("files", "file"), ("fileId", "1234"))

      val result = controllerUnderTest.validateCsvFilesPageSelected()(RequestWithOptionalEmpRef(request, None))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe "form good"
    }

  }

  "performCsvFilesPageSelected" should {
    "reloadWithError if ids are empty" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        override def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList = UpscanCsvFilesList(Seq.empty)
        mockAnyContentAction
      }

      val result = controllerUnderTest.performCsvFilesPageSelected(Seq.empty)(FakeRequest("GET", ""))
      status(result) shouldBe Status.OK
      contentAsString(result) shouldBe "this is a reload"

    }

    "redirect to checkCSVFilePage if everything is good" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        override def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList =
          UpscanCsvFilesList(Seq(UpscanIds(UploadId("value"), "field", UploadedSuccessfully("name", "download"))))
      }
      when(mockSessionCacheRepo.cache[UpscanCsvFilesList](any(), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val result = controllerUnderTest.performCsvFilesPageSelected(Seq.empty)(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
      result.futureValue.header.headers("Location") shouldBe "/check-your-ers-files/choose-and-check-csv-files"

    }
  }

  "cacheUpscanIds" should {
    // TODO: What is this meant to be testing???
    "cache IDs" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockSessionCacheRepo, selectFileTypeView, globalErrorView) {
        mockAnyContentAction
      }
      when(mockSessionCacheRepo.cache[UpscanIds](any(), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val result: Seq[Future[(String, String)]] = controllerUnderTest
        .cacheUpscanIds(Seq(UpscanIds(UploadId("value"), "field", UploadedSuccessfully("name", "download"))))

      assert(result.length == 1)
      Await.result(result.head, Duration.Inf) shouldBe ("", "")
    }
  }
}
