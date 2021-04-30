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

import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import models.CsvFiles
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesList, UpscanIds}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.data.Form
import play.api.http.Status
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents, Request, Result}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec
import views.html.{check_csv_file, check_file, check_file_type, checking_success, format_errors, scheme_type, select_csv_file_types, start}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class CheckCsvFilesControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ErsTestHelper with Injecting {

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

  def buildFakeCheckingServiceController(): CheckingServiceController =
    new CheckingServiceController(mockAuthAction, mockUpscanService, mockSessionService, mcc, mockErsUtil, mockAppConfig,
      formatErrorsView, startView, schemeTypeView, checkFileTypeView, checkCsvFileView, checkFileView, checkingSuccessView) {
      mockAnyContentAction
    }

  "selectCsvFilesPage" should {

    "call showCheckCsvFilesPage if user is authenticated" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def showCheckCsvFilesPage()(
          implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Result] = Future.successful(Ok("Authenticated"))
        mockAnyContentAction
      }
      val result = Await.result(controllerUnderTest.selectCsvFilesPage().apply(Fixtures.buildFakeRequestWithSessionId("GET")), Duration.Inf)
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe "Authenticated"
    }
  }

  "showCheckCsvFilesPage" should {
    "go to global error page if an exception was thrown" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
          InternalServerError("this is very bad")
        mockAnyContentAction
      }
      when(mockErsUtil.remove(refEq(mockErsUtil.CSV_FILES_UPLOAD))(any(), any())).thenReturn(Future.failed(new RuntimeException("this failed tbh")))

      val result = controllerUnderTest.showCheckCsvFilesPage()(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc)
      val failure = Await.result(result, Duration.Inf)

      failure.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      bodyOf(failure) shouldBe "this is very bad"
    }

    "go to the select_csv_file_types page when successful" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def getGlobalErrorPage(implicit request: Request[_], messages: Messages): Result =
          InternalServerError("this is very bad")
        mockAnyContentAction
      }
      when(mockErsUtil.remove(refEq(mockErsUtil.CSV_FILES_UPLOAD))(any(), any())).thenReturn(Future.successful(HttpResponse(Status.OK, "we uploadin")))
      when(mockErsUtil.fetch[String](refEq(mockErsUtil.SCHEME_CACHE))(any(), any(), any(), any()))
        .thenReturn(Future.successful("a string"))
      when(mockErsUtil.getCsvFilesList(any())).thenReturn(Seq(CsvFiles("a file")))
      when(mockErsUtil.getPageElement(any(), any(), any())(any())).thenReturn("this is okay!")

      val result = controllerUnderTest.showCheckCsvFilesPage()(Fixtures.buildEmpRefRequestWithSessionId("GET"), hc)
      val success = Await.result(result, Duration.Inf)

      success.header.status shouldBe Status.OK
      assert(bodyOf(success).contains("this is okay!"))
    }

  }

  "checkCsvFilesPageSelected" should {

    "call validateCsvFilesPageSelected if user is authenticated" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def validateCsvFilesPageSelected()(
          implicit request: RequestWithOptionalEmpRef[AnyContent], hc: HeaderCarrier): Future[Result] = Future.successful(Ok("here we go!"))
        mockAnyContentAction
      }
      val result = Await.result(controllerUnderTest.checkCsvFilesPageSelected().apply(Fixtures.buildFakeRequestWithSessionId("GET")), Duration.Inf)
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe "here we go!"
    }
  }

  "validateCsvFilesPageSelected" should {

    "call reloadWithError if form has errors" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        mockAnyContentAction
      }
      val request = FakeRequest("GET", "").withFormUrlEncodedBody(("files", "file"), ("fileId", "asdasd£$aaa"))

      val result = Await.result(controllerUnderTest.validateCsvFilesPageSelected()(RequestWithOptionalEmpRef(request, None), hc), Duration.Inf)
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe "this is a reload"
    }

    "call reloadWithError if form does not have errors" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def performCsvFilesPageSelected(formData: Seq[CsvFiles])(
          implicit request: Request[AnyRef], hc: HeaderCarrier): Future[Result] = Future.successful(Ok("form good"))
        mockAnyContentAction
      }
      val request = FakeRequest("GET", "").withFormUrlEncodedBody(("files", "file"), ("fileId", "1234"))

      val result = Await.result(controllerUnderTest.validateCsvFilesPageSelected()(RequestWithOptionalEmpRef(request, None), hc), Duration.Inf)
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe "form good"
    }

  }

  "performCsvFilesPageSelected" should {
    "reloadWithError if ids are empty" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        override def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList = UpscanCsvFilesList(Seq.empty)
        mockAnyContentAction
      }

      val result = Await.result(controllerUnderTest.performCsvFilesPageSelected(Seq.empty)(FakeRequest("GET", ""), hc), Duration.Inf)
      status(result) shouldBe Status.OK
      bodyOf(result) shouldBe "this is a reload"

    }

    "redirect to checkCSVFilePage if everything is good" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        override def reloadWithError(form: Option[Form[List[CsvFiles]]])(implicit messages: Messages): Future[Result] =
          Future.successful(Ok("this is a reload"))

        override def createCacheData(csvFilesList: Seq[CsvFiles]): UpscanCsvFilesList =
          UpscanCsvFilesList(Seq(UpscanIds(UploadId("value"), "field", UploadedSuccessfully("name", "download"))))

        override def cacheUpscanIds(ids: Seq[UpscanIds])(implicit request: Request[AnyRef], hc: HeaderCarrier): Seq[Future[CacheMap]] =
          Seq.empty
        mockAnyContentAction
      }
      when(mockErsUtil.cache(refEq(mockErsUtil.CSV_FILES_UPLOAD), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map.empty)))

      val result = Await.result(controllerUnderTest.performCsvFilesPageSelected(Seq.empty)(FakeRequest("GET", ""), hc), Duration.Inf)
      status(result) shouldBe Status.SEE_OTHER
      result.header.headers("Location") shouldBe "/check-your-ers-files/choose-and-check-csv-files"

    }
  }

  "cacheUpscanIds" should {
    "cache IDs" in {
      val controllerUnderTest = new CheckCsvFilesController(mockAuthAction, mcc, mockErsUtil, mockAppConfig, selectFileTypeView) {
        mockAnyContentAction
      }
      when(mockErsUtil.cache[UpscanIds](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map.empty)))

      val result = Await.result(
        controllerUnderTest
          .cacheUpscanIds(Seq(UpscanIds(UploadId("value"), "field", UploadedSuccessfully("name", "download"))))(FakeRequest("GET", ""), hc),
        Duration.Inf)

      assert(result.length == 1)
      Await.result(result.head, Duration.Inf) shouldBe CacheMap("id", Map.empty)
    }
  }

}
