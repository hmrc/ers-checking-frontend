/*
 * Copyright 2025 HM Revenue & Customs
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

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package services
//
//import controllers.Fixtures
//import controllers.auth.{PAYEDetails, RequestWithOptionalEmpRefAndPAYE}
//import helpers.ErsTestHelper
//import models.ERSFileProcessingException
//import org.mockito.ArgumentMatchers
//import org.mockito.ArgumentMatchers._
//import org.mockito.Mockito._
//import org.scalatest.OptionValues
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpecLike
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import play.api.i18n.MessagesImpl
//import play.api.inject.bind
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents}
//import play.api.test.FakeRequest
//import play.api.{Application, i18n}
//import uk.gov.hmrc.models.SheetErrors
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.test.MongoSupport
//import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
//import uk.gov.hmrc.validator.models.SheetErrors
//import uk.gov.hmrc.validator.{DataGenerator, ProcessODSService, StaxProcessor}
//import utils.{ParserUtil, UploadedFileUtil}
//
//import scala.collection.mutable.ListBuffer
//import scala.concurrent.Future
//import scala.util.{Failure, Success}
//
//class ProcessODSServiceSpec
//  extends AnyWordSpecLike
//    with Matchers
//    with OptionValues
//    with ErsTestHelper
//    with GuiceOneAppPerSuite
//    with ScalaFutures
//    with MongoSupport {
//
//  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
//    .configure(
//      Map(
//        "play.i18n.langs" -> List("en", "cy"),
//        "metrics.enabled" -> "false"
//      )
//    )
//    .overrides(
//      bind(classOf[MongoComponent]).toInstance(mongoComponent)
//    ).build()
//
//  val mockUploadedFileUtil: UploadedFileUtil = mock[UploadedFileUtil]
//
//  val config: Map[String, String] = Map(
//    "microservice.services.cachable.short-lived-cache-frontend.host" -> "test",
//    "cachable.short-lived-cache-frontend.port" -> "test",
//    "short-lived-cache-frontend.domain" -> "test"
//  )
//
//  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
//  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
//  implicit val scheme: String = "testScheme"
//  implicit val fakeRequest: RequestWithOptionalEmpRefAndPAYE[AnyContent] = RequestWithOptionalEmpRefAndPAYE(FakeRequest(), None, PAYEDetails(isAgent = false, agentHasPAYEEnrollement = false, None, mockAppConfig))
//
//  def buildProcessODSService(checkODSFileTypeResult: Boolean = true, isValid: Boolean = true): ProcessODSService = {
//    lazy val result = if(isValid) Future.successful(Success(true)) else Future.successful(Success(false))
//    new ProcessODSService(mockUploadedFileUtil, mockSessionCacheRepo, mockErsUtil){
//      when(mockUploadedFileUtil.checkODSFileType(anyString())).thenReturn(checkODSFileTypeResult)
//    }
//  }
//
//  "calling performODSUpload" should {
//
//    val sheetErrors = Fixtures.buildSheetErrors
//
//    "return false if the file has validation errors" in {
////      when(mockDataGenerator.getErrors(any(), any(), any())(any())).thenReturn(sheetErrors)
//      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
//        .thenReturn(Future.successful(("", "")))
//
//      buildProcessODSService(isValid = false).performODSUpload("testFileName", mockStaxProcessor) shouldBe false
//    }
//
//    "return true if the file doesn't have any errors" in {
//      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
//      when(mockDataGenerator.getErrors(any(), any(), any())(any())).thenReturn(emptyErrors)
//      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
//        .thenReturn(Future.successful(("", "")))
//
//      buildProcessODSService().performODSUpload("testFileName", mockStaxProcessor) shouldBe true
//    }
//
//    "return a failure if nothing was found in the cache" in {
//      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
//      when(mockDataGenerator.getErrors(any(), any(), any())(any())).thenReturn(emptyErrors)
//      when(mockSessionCacheRepo.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any()))
//        .thenReturn(Future.failed(new NoSuchElementException))
//
//      assertThrows[NoSuchElementException] { // TODO: Come back to
//        buildProcessODSService().performODSUpload("testFileName", mockStaxProcessor)
//      }
//    }
//
//    "throw an ERSFileProcessingException if the file has an incorrect file type" in {
//      val expectedExceptionMessage = "You chose to check an ODS file, but testFileName isn’t an ODS file."
//
//      val caughtError = intercept[ERSFileProcessingException] { // TODO: Come back to
//        buildProcessODSService(checkODSFileTypeResult = false).performODSUpload("testFileName", mockStaxProcessor)
//      }
//      caughtError.message shouldBe expectedExceptionMessage
//    }
//  }
//
//  "calling checkFileType" should {
//
//    def buildProcessODSService(checkODSFileTypeResult: Boolean = true): ProcessODSService =
//      new ProcessODSService(mockDataGenerator){
//      when(mockUploadedFileUtil.checkODSFileType(anyString())).thenReturn(checkODSFileTypeResult)
//    }
//
//    "return an exception when the file has the incorrect type" in {
//      val exceptionResult: ERSFileProcessingException = intercept[ERSFileProcessingException]{
//        buildProcessODSService(false).checkFileType(mockStaxProcessor, "testFileName")
//      }
//      exceptionResult.message shouldBe "You chose to check an ODS file, but testFileName isn’t an ODS file."
//    }
//
//    "return a ListBuffer of SheetErrors" in {
//      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
//      when(mockDataGenerator.getErrors(any(), any(), any())(any())).thenReturn(emptyErrors)
//
//      buildProcessODSService().checkFileType(mockStaxProcessor, "testFileName") shouldBe emptyErrors
//    }
//  }
//
//  "calling getSheetErrors" should {
//    val schemeErrors = new ListBuffer[SheetErrors]()
//
//    val list1 = ValidationError(Cell("A", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
//    val list2 = ValidationError(Cell("B", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
//    val list3 = ValidationError(Cell("C", 1, "abc"), "001", "error.1", "This entry must be 'yes' or 'no'.")
//
//    val sheetErrors3 = new ListBuffer[ValidationError]()
//    sheetErrors3 += list1
//    sheetErrors3 += list2
//    sheetErrors3 += list3
//    schemeErrors += SheetErrors("sheet_tab_1", sheetErrors3)
//    schemeErrors += SheetErrors("sheet_tab_2", sheetErrors3)
//
//    val sheetErrors1 = new ListBuffer[ValidationError]()
//    sheetErrors1 += list1
//    schemeErrors += SheetErrors("sheet_tab_3", sheetErrors1)
//
//    "return up to the first 100 errors of each sheet" in {
//
//      val processODSService: ProcessODSService = buildProcessODSService(isValid = false)
//      val result = processODSService.getSheetErrors(schemeErrors, 100)
//
//      result.head.errors.size shouldBe 3
//      result(1).errors.size shouldBe 3
//      result(2).errors.size shouldBe 1
//    }
//  }
//
//}
