/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import controllers.Fixtures
import controllers.auth.{PAYEDetails, RequestWithOptionalEmpRefAndPAYE}
import helpers.ErsTestHelper
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesImpl
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.{Application, i18n}
import services.ProcessODSService._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.ods.SheetErrors

import java.io.InputStream
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class ProcessODSServiceSpec
  extends AnyWordSpecLike
    with Matchers
    with OptionValues
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with ScalaFutures
    with MongoSupport {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "play.i18n.langs" -> List("en", "cy"),
        "metrics.enabled" -> "false"
      )
    )
    .overrides(
      bind(classOf[MongoComponent]).toInstance(mongoComponent)
    ).build()

  val config: Map[String, String] = Map(
    "microservice.services.cachable.short-lived-cache-frontend.host" -> "test",
    "cachable.short-lived-cache-frontend.port" -> "test",
    "short-lived-cache-frontend.domain" -> "test"
  )

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  implicit val scheme: String = "testScheme"
  implicit val fakeRequest: RequestWithOptionalEmpRefAndPAYE[AnyContent] = RequestWithOptionalEmpRefAndPAYE(FakeRequest(), None, PAYEDetails(isAgent = false, agentHasPAYEEnrollement = false, None, mockAppConfig))

  def buildProcessODSService(sheetErrors: ListBuffer[SheetErrors]): ProcessODSService = {
    new ProcessODSService(mockSessionCacheRepo, mockErsUtil){
      override def validateOdsFile(fileName: String, processor: InputStream, scheme: String): ListBuffer[SheetErrors] = sheetErrors
    }
  }

  "calling performODSUpload" should {

    val sheetErrors: ListBuffer[SheetErrors] = Fixtures.buildSheetErrors

    "return false if the file has validation errors" in {
      // mocking calls to cache
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))
      when(mockSessionCacheRepo.cache[Long](ArgumentMatchers.eq(mockErsUtil.SCHEME_ERROR_COUNT_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.ERROR_LIST_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val output = buildProcessODSService(sheetErrors)
        .performODSUpload(10, "testFileName.ods", mockInputStream, "csop")
        .futureValue
      output.map(_ shouldBe false)
    }

    "return true if the file doesn't have any errors" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      val output = buildProcessODSService(emptyErrors)
        .performODSUpload(10, "testFileName.ods", mockInputStream, "csop")
        .futureValue

      output.map(_ shouldBe true)
    }

    "return a failure if nothing was found in the cache" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException))

      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))

      val result = buildProcessODSService(emptyErrors).performODSUpload(10, "testFileName.ods", mockInputStream, "csop")
      result.futureValue.swap.map(_ shouldBe a[NoSuchElementException])

    }
  }

  val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
  val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

  val sheetWithThreeErrors = new ListBuffer[ValidationError].addAll(Seq(list1, list2, list3))
  val sheetWithOneError = new ListBuffer[ValidationError].addOne(list1)

  val sheetWithMultipleSchemeError = new ListBuffer[SheetErrors].addAll(
    Seq(
      SheetErrors("sheet_tab_1", sheetWithThreeErrors),
      SheetErrors("sheet_tab_2", sheetWithThreeErrors),
      SheetErrors("sheet_tab_3", sheetWithOneError)
    )
  )

  val sheetWithNoSchemeError = new ListBuffer[SheetErrors].addAll(
    Seq(
      SheetErrors("sheet_tab_1", new ListBuffer[ValidationError]()),
      SheetErrors("sheet_tab_2", new ListBuffer[ValidationError]()),
      SheetErrors("sheet_tab_3", new ListBuffer[ValidationError]())
    )
  )

  "calling getSheetErrors" should {

    "return up to the first 100 errors of each sheet" in {

      val result = getSheetErrors(sheetWithMultipleSchemeError, 100)

      result.head.errors.size shouldBe 3
      result(1).errors.size shouldBe 3
      result(2).errors.size shouldBe 1
    }
  }

  "calling isValid" should {

    "return false if there are no errors in any of the sheetErrors parsed in" in {
      assert(!isValid(sheetWithMultipleSchemeError))
    }

    "return true if there are no errors in any of the sheetErrors parsed in" in {
      assert(isValid(sheetWithNoSchemeError))
    }

  }

}
