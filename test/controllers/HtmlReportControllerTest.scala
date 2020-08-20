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
import models.SheetErrors
import models.upscan.UploadId
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.http.Status
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json
import play.api.libs.json._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{Cell, ValidationError}
import utils.{CacheUtil, PageBuilder}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class HtmlReportControllerTest extends UnitSpec with OneAppPerSuite with MockitoSugar with WithMockedAuthActions {

  val config = Map("application.secret" -> "test",
    "login-callback.url" -> "test",
    "contact-frontend.host" -> "localhost",
    "contact-frontend.port" -> "9250",
    "metrics.enabled" -> false)

  implicit val hc = HeaderCarrier()
  implicit lazy val messages: Messages = Messages(Lang("en"), app.injector.instanceOf[MessagesApi])
  implicit val request: Request[_] = FakeRequest()

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()

  val mockAuthAction : AuthAction = mock[AuthAction]

  "html Error Report Page GET" should {

    def buildFakeHtmlReportController(errorRes: Boolean = true) = new HtmlReportController {
      val mockCacheUtil: CacheUtil = mock[CacheUtil]
      override val cacheUtil: CacheUtil = mockCacheUtil
      when(
        mockCacheUtil.fetch[String](refEq(CacheUtil.SCHEME_CACHE))(any(),any(),any(),any())
      ).thenReturn(
        errorRes match {
          case true => Future.successful("1")
          case _ => Future.failed(new Exception)
        }
      )
      when(
        mockCacheUtil.fetchAll()(any(),any(),any())
      ).thenReturn(
        Future.successful(CacheMap("test", Map(CacheUtil.SCHEME_CACHE -> JsString("test"))))
      )

      override val authAction: AuthAction = mockAuthAction
      mockAnyContentAction
    }

    "gives a call to showHtmlErrorReportPage if user is authenticated" in {
      val controllerUnderTest = buildFakeHtmlReportController()
      val result = controllerUnderTest.htmlErrorReportPage(true).apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.OK
    }
  }

  def buildFakeHtmlReportController() = new HtmlReportController {

    var schemeType = "e"
    var fetchAllMapVal = "e"

    val sheetName = "EMI40_Adjustments_V3"
    var data = ListBuffer(ValidationError(Cell("A",5,"abc"),"001", "error.1", "This entry must be 'yes' or 'no'."))
    data += ValidationError(Cell("D",5,"abc"),"002", "error.2", "This entry must be within the specified number range.")
    data += ValidationError(Cell("F",11,"abc"),"003", "error.3", "This entry must contain 35 characters or less.")
    val schemeSheetErrorList = SheetErrors(sheetName, data)

    override val cacheUtil: CacheUtil = new CacheUtil {

			override def cache[T](key:String, body:T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[_]) = {
        Future.successful(null)
      }

      @throws(classOf[NoSuchElementException])
      override def fetchAll()(implicit hc:HeaderCarrier, ec:ExecutionContext, request: Request[AnyRef]):  Future[CacheMap] = {
        //val map = Map(("mock_scheme" -> Json.toJson("1")),("choose_activity" -> Json.toJson("1")))
        fetchAllMapVal match {
          case "e" => Future(throw new NoSuchElementException)
          case "withErrorListSchemeTypeFileTypeErrorCountSummary" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map((CacheUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList()), (CacheUtil.SCHEME_CACHE -> Json.toJson("csop")), (CacheUtil.FILE_TYPE_CACHE -> Json.toJson(PageBuilder.OPTION_ODS)), (CacheUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(10)), (CacheUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors()))
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
          }
          case "withErrorListSchemeTypeFileTypeZeroErrorCountSummary" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map((CacheUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList()), (CacheUtil.SCHEME_CACHE -> Json.toJson("csop")), (CacheUtil.FILE_TYPE_CACHE -> Json.toJson(PageBuilder.OPTION_ODS)), (CacheUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(0)), (CacheUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors()))
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
          }
          case "withSchemeType" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map(("mock_scheme" -> Json.toJson("csop")))
            val cm : CacheMap = new CacheMap("idcsop", data)
            Future.successful(cm)
          }
        }
      }

      override def shortLivedCache: ShortLivedCache = ???
    }
    override val authAction: AuthAction = mockAuthAction

    mockAnyContentAction
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and nothing in cache" should {
    "throw exception" in {
      val controllerUnderTest = buildFakeHtmlReportController
      contentAsString(
        await(controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"), hc, messages))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage()(request, messages))
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and error count > 0" should {
    "give a status OK and show error report" in {
      val controllerUnderTest = buildFakeHtmlReportController
      controllerUnderTest.fetchAllMapVal = "withErrorListSchemeTypeFileTypeZeroErrorCountSummary"
      val result = controllerUnderTest.showHtmlErrorReportPage(isCsv = true)(Fixtures.buildFakeRequestWithSessionId("GET"),hc, messages)
      status(result) shouldBe Status.OK
    }
  }

  "csvExtractErrors" should {
    val uploadId = UploadId("uploadId")
    "return no errors when none are present" in {
      val cacheMap = CacheMap("uploadId", Map.empty)

      val result = buildFakeHtmlReportController.csvExtractErrors(Seq(uploadId), cacheMap)
      result shouldBe (ListBuffer(), 0, 0)
    }

    "return the errors and count when the sheet has errors" in {
      val errorList = ListBuffer(
        SheetErrors("CSOP_OptionsExercised_V3",
          ListBuffer(
            ValidationError(Cell("A",1,"23-07-2015"),"error.1","001","ers.upload.error.date")
          )
        )
      )
      val errorJson = Json.toJson(errorList)
      val cacheMapWithErrors = CacheMap("uploadId",
        Map(s"error-listUploadId(uploadId)" -> errorJson,
            "scheme-error-countUploadId(uploadId)" -> Json.toJson(1)
      ))

      val result = buildFakeHtmlReportController.csvExtractErrors(Seq(uploadId), cacheMapWithErrors)
      result shouldBe (errorList, 1, 1)
    }
  }
}
