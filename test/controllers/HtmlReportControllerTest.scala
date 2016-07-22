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

import java.util.NoSuchElementException
import java.util.concurrent.TimeoutException
import uk.gov.hmrc.services.validation.{Cell, ValidationError}
import models.SheetErrors
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.{ShortLivedCache, CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.{HttpGet, HttpPut, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CacheUtil, PageBuilder}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.json.JsValue
import play.mvc.Result
import play.api.test.Helpers._

class HtmlReportControllerTest extends UnitSpec with ERSFakeApplication with MockitoSugar {


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
    }

    "give a redirect status (to company authentication frontend) on GET if user is not authenticated" in {
      val controllerUnderTest = buildFakeHtmlReportController()
      val result = controllerUnderTest.htmlErrorReportPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }

    "gives a call to showHtmlErrorReportPage if user is authenticated" in {
      val controllerUnderTest = buildFakeHtmlReportController()
      val result = controllerUnderTest.htmlErrorReportPage().apply(Fixtures.buildFakeRequestWithSessionId("GET"))
      status(result) shouldBe Status.SEE_OTHER
    }

    //    "give a status OK and shows Html Error page if fetch successful and error count > 0" in {
    //      val controllerUnderTest = buildFakeHtmlReportController(errorRes = true)
    //      val result = controllerUnderTest.showHtmlErrorReportPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc)
    //      status(result) shouldBe Status.OK
    //    }
    //
    //    "throws exception if fetch fails" in {
    //      val controllerUnderTest = buildFakeHtmlReportController(errorRes = false)
    //      intercept[Exception] {
    //        Await.result( controllerUnderTest.showHtmlErrorReportPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc), Fixtures.getAwaitDuration)
    //      }
    //    }

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

      override def cache[T](key:String, body:T)(implicit hc:HeaderCarrier, ec:ExecutionContext, formats: json.Format[T], request: Request[AnyRef]) = {
        Future.successful(null)
      }

      @throws(classOf[NoSuchElementException])
      override def fetchAll()(implicit hc:HeaderCarrier, ec:ExecutionContext, request: Request[AnyRef]):  Future[CacheMap] = {
        //val map = Map(("mock_scheme" -> Json.toJson("1")),("choose_activity" -> Json.toJson("1")))
        fetchAllMapVal match {
          case "e" => Future(throw new NoSuchElementException)
          case "withErrorListSchemeTypeFileTypeErrorCountSummary" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map((CacheUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList()), (CacheUtil.SCHEME_CACHE -> Json.toJson("1")), (CacheUtil.FILE_TYPE_CACHE -> Json.toJson(PageBuilder.OPTION_ODS)), (CacheUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(10)), (CacheUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors()))
            val cm : CacheMap = new CacheMap("id1", data)
            Future.successful(cm)
          }
          case "withErrorListSchemeTypeFileTypeZeroErrorCountSummary" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map((CacheUtil.ERROR_LIST_CACHE -> Fixtures.getMockErrorList()), (CacheUtil.SCHEME_CACHE -> Json.toJson("1")), (CacheUtil.FILE_TYPE_CACHE -> Json.toJson(PageBuilder.OPTION_ODS)), (CacheUtil.SCHEME_ERROR_COUNT_CACHE -> Json.toJson(0)), (CacheUtil.ERROR_SUMMARY_CACHE -> Fixtures.getMockSummaryErrors()))
            val cm : CacheMap = new CacheMap("id1", data)
            Future.successful(cm)
          }
          case "withSchemeType" => {

            val data : scala.Predef.Map[scala.Predef.String, play.api.libs.json.JsValue] = Map(("mock_scheme" -> Json.toJson("1")))
            val cm : CacheMap = new CacheMap("id1", data)
            Future.successful(cm)
          }
        }
      }

      override def shortLivedCache: ShortLivedCache = ???
    }
  }


  // html error page
  "Calling HtmlReportController.htmlErrorReportPage without authentication" should {
    "give a redirect status (to company authentication frontend)" in {
      val controllerUnderTest = buildFakeHtmlReportController
      val result = controllerUnderTest.htmlErrorReportPage().apply(FakeRequest("GET", ""))
      status(result) shouldBe Status.SEE_OTHER
    }
  }
  //
  //  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, an error list, scheme type, error count, and summary in cache" should {
  //    "render html error report page)" in {
  //      val controllerUnderTest = buildFakeHtmlReportController
  //      controllerUnderTest.fetchAllMapVal = "withErrorListSchemeTypeFileTypeErrorCountSummary"
  //      val result = controllerUnderTest.showHtmlErrorReportPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"),hc)
  //      status(result) shouldBe Status.OK
  //    }
  //  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and nothing in cache" should {
    "throw exception" in {
      val controllerUnderTest = buildFakeHtmlReportController
      contentAsString(await(controllerUnderTest.showHtmlErrorReportPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"), hc))) shouldBe contentAsString(controllerUnderTest.getGlobalErrorPage)
    }
  }

  "Calling HtmlReportController.showHtmlErrorReportPage with authentication, and error count > 0" should {
    "give a status OK and show error report" in {
      val controllerUnderTest = buildFakeHtmlReportController
      controllerUnderTest.fetchAllMapVal = "withErrorListSchemeTypeFileTypeZeroErrorCountSummary"
      val result = controllerUnderTest.showHtmlErrorReportPage(Fixtures.buildFakeUser, Fixtures.buildFakeRequestWithSessionId("GET"),hc)
      status(result) shouldBe Status.OK
//      result.header.headers.get("Location").get shouldBe routes.CheckingServiceController.checkingSuccessPage.toString()
    }
  }
}
