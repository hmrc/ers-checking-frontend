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
import models.ERSFileProcessingException
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
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.ods.SheetErrors
import utils.UploadedFileUtil

import java.io.InputStream
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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

  val mockUploadedFileUtil: UploadedFileUtil = mock[UploadedFileUtil]

  val config: Map[String, String] = Map(
    "microservice.services.cachable.short-lived-cache-frontend.host" -> "test",
    "cachable.short-lived-cache-frontend.port" -> "test",
    "short-lived-cache-frontend.domain" -> "test"
  )

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  implicit val scheme: String = "testScheme"
  implicit val fakeRequest: RequestWithOptionalEmpRefAndPAYE[AnyContent] = RequestWithOptionalEmpRefAndPAYE(FakeRequest(), None, PAYEDetails(isAgent = false, agentHasPAYEEnrollement = false, None, mockAppConfig))

  def buildProcessODSService(checkODSFileTypeResult: Boolean = true, sheetErrors: ListBuffer[SheetErrors]): ProcessODSService = {
    new ProcessODSService(mockUploadedFileUtil, mockSessionCacheRepo, mockErsUtil){
      when(mockUploadedFileUtil.checkODSFileType(anyString())).thenReturn(checkODSFileTypeResult)
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

      val output: Future[Try[Boolean]] = buildProcessODSService(checkODSFileTypeResult = true, sheetErrors)
        .performODSUpload(10, "testFileName", mockInputStream, "csop")
      output.futureValue shouldBe Success(false)
    }

    "return true if the file doesn't have any errors" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.successful(("", "")))

      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      val output: Future[Try[Boolean]] = buildProcessODSService(checkODSFileTypeResult = true, emptyErrors)
        .performODSUpload(10, "testFileName", mockInputStream, "csop")

      output.futureValue shouldBe Success(true)
    }

    "return a failure if nothing was found in the cache" in {
      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException))

      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      val result: Try[Boolean] = buildProcessODSService(checkODSFileTypeResult = true, emptyErrors)
        .performODSUpload(10, "testFileName", mockInputStream, "csop")
        .futureValue

      assert(result.isFailure)
      result.failed.get shouldBe a[NoSuchElementException]

    }

    "throw an ERSFileProcessingException if the file has an incorrect file type" in {
      val exception = ERSFileProcessingException("You chose to check an ODS file, but testFileName isn’t an ODS file.",
        "You chose to check an ODS file, but testFileName isn’t an ODS file.")
      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))

      buildProcessODSService(checkODSFileTypeResult = false, emptyErrors)
        .performODSUpload(10, "testFileName", mockInputStream, "csop").futureValue shouldBe Failure(exception)
    }
  }

  "calling checkFileType" should {

    "return an exception when the file has the incorrect type" in {

      val processODSService: ProcessODSService = new ProcessODSService(mockUploadedFileUtil, mockSessionCacheRepo, mockErsUtil)
      val exceptionResult: ERSFileProcessingException = intercept[ERSFileProcessingException](processODSService.checkFileType("testFileName"))
      exceptionResult.message shouldBe "You chose to check an ODS file, but testFileName isn’t an ODS file."
    }

    "return Unit if the file is an ODS file" in {
      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      buildProcessODSService(checkODSFileTypeResult = true, emptyErrors).checkFileType("testFileName.ods") shouldBe () // Should not throw exception if file is an ODS file
    }
  }

  "calling getSheetErrors" should {
    val schemeErrors = new ListBuffer[SheetErrors]()

    val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
    val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")

    val sheetErrors3 = new ListBuffer[ValidationError]()
    sheetErrors3 += list1
    sheetErrors3 += list2
    sheetErrors3 += list3
    schemeErrors += SheetErrors("sheet_tab_1", sheetErrors3)
    schemeErrors += SheetErrors("sheet_tab_2", sheetErrors3)

    val sheetErrors1 = new ListBuffer[ValidationError]()
    sheetErrors1 += list1
    schemeErrors += SheetErrors("sheet_tab_3", sheetErrors1)

    "return up to the first 100 errors of each sheet" in {

      val processODSService: ProcessODSService = buildProcessODSService(checkODSFileTypeResult = true, schemeErrors)
      val result = processODSService.getSheetErrors(schemeErrors, 100)

      result.head.errors.size shouldBe 3
      result(1).errors.size shouldBe 3
      result(2).errors.size shouldBe 1
    }
  }

}
