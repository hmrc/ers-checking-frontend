/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.auth.RequestWithOptionalEmpRef
import helpers.ErsTestHelper
import models.{ERSFileProcessingException, SheetErrors}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesImpl
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, DefaultMessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.{Application, i18n}
import uk.gov.hmrc.http.cache.client.CacheMap
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.services.validation.models.ValidationError
import utils.{ParserUtil, UploadedFileUtil}

import java.util.NoSuchElementException
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}
import org.scalatest.wordspec.AnyWordSpecLike

class ProcessODSServiceSpec extends AnyWordSpecLike with Matchers with OptionValues with ErsTestHelper with GuiceOneAppPerSuite with ScalaFutures {

  val mockUploadedFileUtil: UploadedFileUtil = mock[UploadedFileUtil]
  val mockDataGenerator: DataGenerator = mock[DataGenerator]
  val mockStaxProcessor: StaxProcessor = mock[StaxProcessor]

  val config = Map(
    "microservice.services.cachable.short-lived-cache-frontend.host" -> "test",
    "cachable.short-lived-cache-frontend.port" -> "test",
    "short-lived-cache-frontend.domain" -> "test"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder().configure(config).build()
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  lazy val mockParserUtil: ParserUtil = mock[ParserUtil]
  lazy val testParserUtil: ParserUtil = app.injector.instanceOf[ParserUtil]
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)
  implicit val scheme: String = "testScheme"
  implicit val fakeRequest: RequestWithOptionalEmpRef[AnyContent] = RequestWithOptionalEmpRef(FakeRequest(), None)

  "calling performODSUpload" should {

    def buildProcessODSService(checkODSFileTypeResult: Boolean = true, isValid: Boolean = true): ProcessODSService = {
      lazy val result = if(isValid) Future.successful(Success(true)) else Future.successful(Success(false))
      new ProcessODSService(mockUploadedFileUtil, mockParserUtil, mockDataGenerator, mockErsUtil){
        when(mockParserUtil.isFileValid(any(), any())(any())).thenReturn(result)
        when(mockUploadedFileUtil.checkODSFileType(anyString())).thenReturn(checkODSFileTypeResult)
      }
    }

    val sheetErrors = Fixtures.buildSheetErrors

    "return false if the file has validation errors" in {
      when(mockDataGenerator.getErrors(any(), any(), any())(any(), any(), any())).thenReturn(sheetErrors)
      when(mockErsUtil.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      buildProcessODSService(isValid = false).performODSUpload("testFileName", mockStaxProcessor).futureValue shouldBe Success(false)
    }

    "return true if the file doesn't have any errors" in {
      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      when(mockDataGenerator.getErrors(any(), any(), any())(any(), any(), any())).thenReturn(emptyErrors)
      when(mockErsUtil.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any(), any()))
        .thenReturn(Future.successful(CacheMap("id", Map())))

      buildProcessODSService().performODSUpload("testFileName", mockStaxProcessor).futureValue shouldBe Success(true)
    }

    "return a failure if nothing was found in the cache" in {
      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      when(mockDataGenerator.getErrors(any(), any(), any())(any(), any(), any())).thenReturn(emptyErrors)
      when(mockErsUtil.cache[String](ArgumentMatchers.eq(mockErsUtil.FILE_NAME_CACHE), any())(any(), any(), any()))
        .thenReturn(Future.failed(new NoSuchElementException))

      val result = buildProcessODSService().performODSUpload("testFileName", mockStaxProcessor).futureValue
      assert(result.isFailure)
      result.failed.get shouldBe a[NoSuchElementException]
    }

    "throw an ERSFileProcessingException if the file has an incorrect file type" in {
      val exception = ERSFileProcessingException("You chose to check an ODS file, but testFileName isn’t an ODS file.",
        "You chose to check an ODS file, but testFileName isn’t an ODS file.")

      buildProcessODSService(false).performODSUpload("testFileName", mockStaxProcessor).futureValue shouldBe Failure(exception)
    }
  }

  "calling checkFileType" should {

    def buildProcessODSService(checkODSFileTypeResult: Boolean = true): ProcessODSService =
      new ProcessODSService(mockUploadedFileUtil, testParserUtil, mockDataGenerator, mockErsUtil){
      when(mockUploadedFileUtil.checkODSFileType(anyString())).thenReturn(checkODSFileTypeResult)
    }

    "return an exception when the file has the incorrect type" in {
      val exceptionResult: ERSFileProcessingException = intercept[ERSFileProcessingException]{
        buildProcessODSService(false).checkFileType(mockStaxProcessor, "testFileName")
      }
      exceptionResult.message shouldBe "You chose to check an ODS file, but testFileName isn’t an ODS file."
    }

    "return a ListBuffer of SheetErrors" in {
      val emptyErrors = ListBuffer[SheetErrors](SheetErrors("testName", ListBuffer[ValidationError]()))
      when(mockDataGenerator.getErrors(any(), any(), any())(any(), any(), any())).thenReturn(emptyErrors)

      buildProcessODSService().checkFileType(mockStaxProcessor, "testFileName") shouldBe emptyErrors
    }
  }
}
