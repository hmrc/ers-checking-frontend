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
import models.SheetErrors
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
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
import play.api.mvc.{DefaultMessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import play.api.{Application, i18n}
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.CacheUtil
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
  // scalastyle:off magic.number

  val config: Map[String, Any] = Map(
    "application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  val view: html_error_report = inject[html_error_report]
  val globalErrorView: global_error = inject[global_error]

  val uploadId: UploadId = UploadId("uploadId")

  "html Error Report Page GET" should {
    "gives a call to showHtmlErrorReportPage if user is authenticated" in {
      lazy val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(
        files = List(UpscanCsvFilesCallback(uploadId, UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))))
      )

      lazy val storedValues: Seq[(String, JsValue)] = Seq(
        "scheme-error-count" -> JsString("test"),
        "error-list" -> JsString("test"),
        "scheme-type" -> JsString("csop"),
        "callback_data_key_csv" -> Json.toJson(upscanCsvFilesListCallbackList)
      )

      val cacheItem = generateTestCacheItem(id = "test", data = storedValues)
      mockAnyContentAction

      when(mockSessionCacheRepo.fetchAll()(any())).thenReturn(Future.successful(cacheItem))
      when(mockErsUtil.getSchemeName(any())).thenReturn(("ers_pdf_error_report.csop", "CSOP"))

      lazy val result = controllerUnderTest.htmlErrorReportPage(true).apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and nothing in cache" should {
    "throw exception" in {
      val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
      when(mockSessionCacheRepo.fetchAll()(any())).thenReturn(Future(throw new NoSuchElementException))
      val res: Future[Result] = controllerUnderTest
        .showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), testMessages)
      res.map { result =>
        result shouldBe contentAsString(Future(controllerUnderTest.getGlobalErrorPage(request, testMessages)))
      }
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and error count > 0" should {
    "give a status 500 and show error report" in {
      val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
      val cacheItem: CacheItem = generateTestCacheItem(
        id = "idcsop",
        data = Seq(
          mockErsUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList,
          mockErsUtil.SCHEME_CACHE -> Json.toJson("csop"),
          mockErsUtil.FILE_TYPE_CACHE -> Json.toJson(mockErsUtil.OPTION_ODS),
          mockErsUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(0),
          mockErsUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors
        )
      )

      when(mockSessionCacheRepo.fetchAll()(any())).thenReturn(Future.successful(cacheItem))

      val result = controllerUnderTest
        .showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), testMessages)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "csvExtractErrors" should {
    "return no errors when none are present" in {
      val cacheItem = generateTestCacheItem("uploadId")

      val result = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
        .csvExtractErrors(Seq(uploadId), cacheItem)
      result shouldBe ((ListBuffer(), 0, 0))
    }

    "return the errors and count when the sheet has errors" in {
      val errorList: ListBuffer[SheetErrors] = ListBuffer(
        SheetErrors("CSOP_OptionsExercised_V4",
          ListBuffer(
            ValidationError(Cell("A", 1, "23-07-2015"), "error.1", "001", "ers.upload.error.date")
          )
        )
      )
      val cacheItemWithErrors = generateTestCacheItem(
        id = uploadId.value,
        data = Seq(
        s"$ERROR_LIST_CACHE${uploadId.value}" -> Json.toJson(errorList),
        s"$SCHEME_ERROR_COUNT_CACHE${uploadId.value}" -> Json.toJson(1)
      ))

      val result = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
        .csvExtractErrors(Seq(uploadId), cacheItemWithErrors)
      result shouldBe ((errorList, 1, 1))
    }

    "display the all the errors and send unique error keys while calling audit" in {
      val errorList: ListBuffer[SheetErrors] = ListBuffer(
        SheetErrors("CSOP_OptionsExercised_V4",
          ListBuffer(
            ValidationError(Cell("A", 1, "23-07-2015"), "error.1", "001", "ers.upload.error.date")
          )
        ),
        SheetErrors("CSOP_OptionsExercised_V4",
          ListBuffer(
            ValidationError(Cell("F", 6, "yes"), "error.6", "006", "ers.upload.error.yes-no")
          )
        ),
        SheetErrors("CSOP_OptionsExercised_V4",
          ListBuffer(
            ValidationError(Cell("A", 1, "23-07-2015"), "error.1", "001", "ers.upload.error.date")
          )
        )
      )

      val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(
        files = List(UpscanCsvFilesCallback(uploadId, UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))))
      )
      val cacheItemWithErrors = generateTestCacheItem(
        id = uploadId.value,
        data = Seq(
          "callback_data_key_csv" -> Json.toJson(upscanCsvFilesListCallbackList),
          s"$ERROR_LIST_CACHE${uploadId.value}" -> Json.toJson(errorList),
          s"$SCHEME_ERROR_COUNT_CACHE${uploadId.value}" -> Json.toJson(1)
        ))

      when(mockSessionCacheRepo.fetchAll()(any())).thenReturn(Future.successful(cacheItemWithErrors))
      when(mockErsUtil.getSchemeName(any())).thenReturn(("ers_pdf_error_report.csop", "CSOP"))
      val res = controllerUnderTest
        .showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), testMessages)
      res.map { result =>
        result shouldBe contentAsString(controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(request, testMessages))
      }
    }

  }

  "getEntry" must {
    "return None when key isn't found" in {
      val data = generateTestCacheItem(
        "sessionId",
        Seq(
          "name" -> JsString("some-name"),
          "downloadUrl" -> JsString("some-url")
        )
      )
      val result = new HtmlReportController(mockAuthAction, mcc, mockSessionCacheRepo, view, mockAuditEvents, globalErrorView)
        .getEntry[UploadedSuccessfully](data, "key-not-found")

      result shouldBe None
    }
  }
}
