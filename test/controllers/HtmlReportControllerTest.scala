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

import utils.CacheUtil
import helpers.ErsTestHelper
import models.SheetErrors
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.MessagesImpl
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.mvc.{DefaultMessagesControllerComponents, Request, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import play.api.{Application, i18n}
import repository.ERSSessionCacheRepository
import services.SessionCacheService
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import views.html.{global_error, html_error_report}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class HtmlReportControllerTest
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with GuiceOneAppPerSuite
    with ErsTestHelper
    with Injecting
    with CacheUtil {

  val config: Map[String, Any] = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)


  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  val view: html_error_report = inject[html_error_report]
  val globalErrorView: global_error = inject[global_error]

  "html Error Report Page GET" should {

    "gives a call to showHtmlErrorReportPage if user is authenticated" in {
      lazy val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheService, view, globalErrorView)
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))
      val upscanCsvFilesCallback = UpscanCsvFilesCallback(UploadId("uploadId"), successfully)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

      lazy val storedValues: Seq[(String, JsValue)] = Seq(
        "scheme-error-count" -> JsString("test"),
          "error-list" -> JsString("test"),
          "scheme-type" -> JsString("test"),
          "callback_data_key_csv" -> Json.toJson(upscanCsvFilesListCallbackList)
      )

      val cacheItem = generateTestCacheItem(id = "test", data = storedValues)
      mockAnyContentAction

      when(mockSessionCacheService.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockSessionCacheService.getEntry[String](any(), ArgumentMatchers.eq(SCHEME_CACHE))(any()))
        .thenReturn(Some("csop"))
      when(mockErsUtil.getSchemeName(any())).thenReturn(("ers_pdf_error_report.csop", "CSOP"))
      when(mockSessionCacheService.getEntry[UpscanCsvFilesCallbackList](any(), ArgumentMatchers.eq(CALLBACK_DATA_KEY_CSV))(any()))
        .thenReturn(Some(upscanCsvFilesListCallbackList))
      when(mockSessionCacheService.getEntry[ListBuffer[SheetErrors]](any(), ArgumentMatchers.eq(ERROR_LIST_CACHE + "uploadId"))(any()))
        .thenReturn(Some(ListBuffer()))
      when(mockSessionCacheService.getEntry[Long](any(), ArgumentMatchers.eq(SCHEME_ERROR_COUNT_CACHE + "uploadId"))(any())).thenReturn(Some(0L))

      lazy val result = controllerUnderTest.htmlErrorReportPage(true).apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }
  }

  def buildFakeHtmlReportController(fetchAllMapVal: String = "e"): HtmlReportController = {
    val mockSessionRepository = mock[ERSSessionCacheRepository]
    class TestSessionCacheService extends SessionCacheService(mockSessionRepository)(ec) {
      val FIVE = 5
      val ELEVEN = 11
      val TEN = 10
      var data: ListBuffer[ValidationError] = ListBuffer(ValidationError(Cell("A", FIVE, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'."))
      data += ValidationError(Cell("D", FIVE, "abc"), "002", "error.2", "This entry must be within the specified number range.")
      data += ValidationError(Cell("F", ELEVEN, "abc"), "003", "error.3", "This entry must contain 35 characters or less.")

      override def cache[T](key: String, body: T)(implicit request: Request[_], formats: Format[T]): Future[(String, String)] = {
        Future.successful(null)
      }

      override def fetchAll()(implicit request: Request[_]): Future[CacheItem] = {
        fetchAllMapVal match {
          case "e" => Future(throw new NoSuchElementException)
          case "withErrorListSchemeTypeFileTypeErrorCountSummary" =>
            val data: Seq[(String, JsValue)] = Seq(
              mockErsUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList,
              mockErsUtil.SCHEME_CACHE -> Json.toJson("csop"),
              mockErsUtil.FILE_TYPE_CACHE -> Json.toJson(mockErsUtil.OPTION_ODS),
              mockErsUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(TEN),
              mockErsUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors)
            val cacheItem: CacheItem = generateTestCacheItem("idcsop", data)
            Future.successful(cacheItem)
          case "withErrorListSchemeTypeFileTypeZeroErrorCountSummary" =>
            val data: Seq[(String, JsValue)] = Seq(
              mockErsUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList,
              mockErsUtil.SCHEME_CACHE -> Json.toJson("csop"),
              mockErsUtil.FILE_TYPE_CACHE -> Json.toJson(mockErsUtil.OPTION_ODS),
              mockErsUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(0),
              mockErsUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors
            )
            val cacheItem: CacheItem = generateTestCacheItem("idcsop", data)
            Future.successful(cacheItem)
          case "withSchemeType" =>
            val data: Seq[(String, JsValue)] = Seq("mock_scheme" -> Json.toJson("csop"))
            val cacheItem: CacheItem = generateTestCacheItem("idcsop", data)
            Future.successful(cacheItem)
        }
      }
    }

    new HtmlReportController(mockAuthAction, mcc, new TestSessionCacheService, view, globalErrorView)(
      implicitly, mockErsUtil, mockAppConfig) {
      mockAnyContentAction
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and nothing in cache" should {
    "throw exception" in {
      val controllerUnderTest = buildFakeHtmlReportController()
      val res: Future[Result] = controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), testMessages)
      res.map { result =>
        result shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
      }
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and error count > 0" should {
    "give a status 500 and show error report" in {
      val controllerUnderTest = buildFakeHtmlReportController("withErrorListSchemeTypeFileTypeZeroErrorCountSummary")
      val result = controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), testMessages)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "csvExtractErrors" should {
    val uploadId = UploadId("uploadId")
    "return no errors when none are present" in {
      val cacheItem = generateTestCacheItem("uploadId")

      val result = buildFakeHtmlReportController().csvExtractErrors(Seq(uploadId), cacheItem)
      result shouldBe ((ListBuffer(), 0, 0))
    }

    "return the errors and count when the sheet has errors" in {
      val errorList = ListBuffer(
        SheetErrors("CSOP_OptionsExercised_V4",
          ListBuffer(
            ValidationError(Cell("A", 1, "23-07-2015"), "error.1", "001", "ers.upload.error.date")
          )
        )
      )
      val errorJson = Json.toJson(errorList)
      val cacheItemWithErrors = generateTestCacheItem(
        id = "uploadId",
        data = Seq(
        s"error-list${uploadId.value}" -> errorJson,
        s"scheme-error-count${uploadId.value}" -> Json.toJson(1)
      ))

      val result = buildFakeHtmlReportController().csvExtractErrors(Seq(uploadId), cacheItemWithErrors)
      result shouldBe ((errorList, 1, 1))
    }
  }
}
