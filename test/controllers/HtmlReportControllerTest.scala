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

import config.ERSShortLivedCache
import helpers.ErsTestHelper
import models.SheetErrors
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.MessagesImpl
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.libs.json._
import play.api.mvc.{DefaultMessagesControllerComponents, Request, Result}
import play.api.test.Helpers._
import play.api.{Application, i18n}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.{ERSUtil, HtmlCreator}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class HtmlReportControllerTest extends UnitSpec with GuiceOneAppPerSuite with ErsTestHelper {

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(app)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  val mockHtmlCreator: HtmlCreator = mock[HtmlCreator]
  val mockErsShortLivedCache: ERSShortLivedCache = mock[ERSShortLivedCache]

  "html Error Report Page GET" should {

    "gives a call to showHtmlErrorReportPage if user is authenticated" in {
      lazy val controllerUnderTest = new HtmlReportController(mockAuthAction, mcc, mockHtmlCreator, mockErsUtil, mockAppConfig)
      val successfully: UploadedSuccessfully = UploadedSuccessfully("thefilename", "downloadUrl", Some(1000))
      val upscanCsvFilesCallback= UpscanCsvFilesCallback(UploadId("uploadId"), successfully)
      val upscanCsvFilesListCallbackList = UpscanCsvFilesCallbackList(files = List(upscanCsvFilesCallback))

      lazy val storedValues = Map(
        "scheme-error-count" -> JsString("test"),
        "error-list" -> JsString("test"),
        "scheme-type" -> JsString("test"),
        "callback_data_key_csv" -> Json.toJson(upscanCsvFilesListCallbackList)
      )

      mockAnyContentAction
      when(mockErsUtil.getSchemeName(any())).thenReturn(("ers_pdf_error_report.csop", "CSOP"))
      when(mockErsUtil.fetchAll()(any(), any(), any())).thenReturn(Future.successful(CacheMap("test", storedValues)))
      when(mockHtmlCreator.getSheets(any())(any())).thenReturn("something")

      lazy val result = controllerUnderTest.htmlErrorReportPage(true).apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }
  }

  def buildFakeHtmlReportController(fetchAllMapVal: String = "e"): HtmlReportController = {
    class TestERSUtil extends ERSUtil(mockErsShortLivedCache){
      val FIVE = 5
      val ELEVEN = 11
      val TEN = 10
      var data: ListBuffer[ValidationError] = ListBuffer(ValidationError(Cell("A",FIVE,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'."))
      data += ValidationError(Cell("D",FIVE,"abc"),"002", "error.2", "This entry must be within the specified number range.")
      data += ValidationError(Cell("F",ELEVEN,"abc"),"003", "error.3", "This entry must contain 35 characters or less.")

      override def cache[T](key:String, body:T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[_]) = {
        Future.successful(null)
      }

      @throws(classOf[NoSuchElementException])
      override def fetchAll()(implicit hc:HeaderCarrier, ec:ExecutionContext, request: Request[AnyRef]):  Future[CacheMap] = {
        fetchAllMapVal match {
          case "e" => Future(throw new NoSuchElementException)
          case "withErrorListSchemeTypeFileTypeErrorCountSummary" =>
            val data : Map[String, JsValue] = Map(
              mockErsUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList,
              mockErsUtil.SCHEME_CACHE -> Json.toJson("csop"),
              mockErsUtil.FILE_TYPE_CACHE -> Json.toJson(mockErsUtil.OPTION_ODS),
              mockErsUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(TEN),
              mockErsUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors)
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
          case "withErrorListSchemeTypeFileTypeZeroErrorCountSummary" =>
            val data : Map[String, JsValue] = Map(
              mockErsUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList,
              mockErsUtil.SCHEME_CACHE -> Json.toJson("csop"),
              mockErsUtil.FILE_TYPE_CACHE -> Json.toJson(mockErsUtil.OPTION_ODS),
              mockErsUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(0),
              mockErsUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors
            )
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
          case "withSchemeType" =>
            val data : Map[String, JsValue] = Map("mock_scheme" -> Json.toJson("csop"))
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
        }
      }
    }

    new HtmlReportController(mockAuthAction, mcc, mockHtmlCreator, new TestERSUtil, mockAppConfig) {
      mockAnyContentAction
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and nothing in cache" should {
    "throw exception" in {
      val controllerUnderTest = buildFakeHtmlReportController()
      val res: Future[Result] = controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), hc, testMessages)
      res.map { result =>
        result shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage(request, testMessages))}
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and error count > 0" should {
    "give a status 500 and show error report" in {
      val controllerUnderTest = buildFakeHtmlReportController("withErrorListSchemeTypeFileTypeZeroErrorCountSummary")
      val result = controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"),hc, testMessages)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "csvExtractErrors" should {
    val uploadId = UploadId("uploadId")
    "return no errors when none are present" in {
      val cacheMap = CacheMap("uploadId", Map.empty)

      val result = buildFakeHtmlReportController().csvExtractErrors(Seq(uploadId), cacheMap)
      result shouldBe (ListBuffer(), 0, 0)
    }

//    "return the errors and count when the sheet has errors" in {
//      val errorList = ListBuffer(
//        SheetErrors("CSOP_OptionsExercised_V3",
//          ListBuffer(
//            ValidationError(Cell("A",1,"23-07-2015"),"error.1","001","ers.upload.error.date")
//          )
//        )
//      )
//      val errorJson = Json.toJson(errorList)
//      val cacheMapWithErrors = CacheMap("uploadId",
//        Map(s"error-listUploadId(uploadId)" -> errorJson,
//            "scheme-error-countUploadId(uploadId)" -> Json.toJson(1)
//      ))
//
//      val result = buildFakeHtmlReportController().csvExtractErrors(Seq(uploadId), cacheMapWithErrors)
//      result shouldBe (errorList, 1, 1)
//    }
  }
}
