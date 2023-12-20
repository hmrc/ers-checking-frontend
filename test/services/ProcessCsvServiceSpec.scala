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

package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.IOResult
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import helpers.ErsTestHelper
import models.upscan.{UploadId, UploadedSuccessfully, UpscanCsvFilesCallback, UpscanCsvFilesCallbackList}
import models.{ERSFileProcessingException, RowValidationResults, SheetErrors}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{ScalaFutures, TimeLimits}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, i18n}
import play.api.i18n.{Messages, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.DefaultMessagesControllerComponents
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.services.validation.DataValidator
import uk.gov.hmrc.services.validation.models.{Cell, ValidationError}
import utils.CsvParserUtil

import java.io.File
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class ProcessCsvServiceSpec
  extends TestKit(ActorSystem("Test"))
    with AnyWordSpecLike
    with Matchers
    with OptionValues
    with EitherValues
    with ErsTestHelper
    with GuiceOneAppPerSuite
    with TimeLimits
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

  def convertToAkkaSource(file: File): Source[List[ByteString], Future[IOResult]] = {
    FileIO.fromPath(file.toPath)
      .via(CsvParsing.lineScanner())
  }

  lazy val testParserUtil: CsvParserUtil = fakeApplication.injector.instanceOf[CsvParserUtil]
  val mockDataGenerator: DataGenerator = mock[DataGenerator]
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  def testProcessCsvService: ProcessCsvService = new ProcessCsvService(testParserUtil, mockDataGenerator, mockAppConfig,
    mockSessionCacheRepo, mockErsUtil, mockErsValidator)

  "processRow" should {

    "process a row and return the errors it contains" in {
      val source = convertToAkkaSource(new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V4.csv"))
      val dataValidator = new DataValidator(ConfigFactory.load.getConfig("ers-other-grants-validation-config"))
      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(Some(List(ValidationError(Cell("A", 0, "25  2015"), "error.1", "001", "ers.upload.error.date"))))
      when(mockErsValidator.getCells(any(), any()))
        .thenReturn(Seq(Cell("A", 0, "25  2015")))

      val resultFuture = source
        .runWith(Sink.seq).map { fileCopied =>
        testProcessCsvService.processRow(fileCopied.flatten.toList, "Other_Grants_V4.csv", dataValidator)
      }

      val result = Await.result(resultFuture, Duration.Inf)
      assert(result.isRight)
      result.value.validationErrors.head.cell shouldBe Cell("A", 0, "25  2015")
      result.value.validationErrors.head.errorId shouldBe "001"
      result.value.validationErrors.head.errorMsg shouldBe "ers.upload.error.date"
      result.value.validationErrors.head.ruleId shouldBe "error.1"
    }

    "process a row and return an empty list if there are no errors" in {
      val source = convertToAkkaSource(new File(System.getProperty("user.dir") + "/test/resources/copy/CSOP_OptionsGranted_V4.csv"))
      val dataValidator = new DataValidator(ConfigFactory.load.getConfig("ers-csop-granted-validation-config"))
      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(None)
      when(mockErsValidator.getCells(any(), any()))
        .thenReturn(Seq(Cell("A", 0, "2015-09-23"), Cell("B", 0, "250"),
          Cell("C", 0, "123.12"), Cell("D", 0, "12.1234"), Cell("E", 0, "12.1234"),
          Cell("F", 0, "no"), Cell("G", 0, "yes"),
          Cell("H", 0, "AB12345678"), Cell("I", 0, "no")
        ))

      val resultFuture = source
        .runWith(Sink.seq).map { fileCopied =>
        testProcessCsvService.processRow(fileCopied.flatten.toList, "CSOP_OptionsGranted_V4.csv", dataValidator)
      }

      val result = Await.result(resultFuture, Duration.Inf)
      assert(result.isRight)
      result.value.validationErrors.isEmpty shouldBe true
      }
  }

  "processFiles" should {

    when(mockDataGenerator.getSheetCsv(any(), any())(any())).thenReturn(Right((ERSTemplatesInfo.ersSheets("CSOP_OptionsGranted_V4"), "csop")))
    when(mockDataGenerator.identifyAndDefineSheetCsv(any())(any(), any())).thenReturn(Right("CSOP_OptionsGranted_V4"))
    when(mockDataGenerator.setValidatorCsv(any())(any(), any())).thenReturn(Right(new DataValidator(ConfigFactory.load.getConfig("ers-csop-granted-validation-config"))))
    when(mockErsUtil.SCHEME_ERROR_COUNT_CACHE).thenReturn("10")
    when(mockSessionCacheRepo.cache[Any](any(), any())(any(), any())).thenReturn(Future(("", "")))

    def returnStubSource(x: String, data: String): Source[HttpResponse, NotUsed] = {
      Source.single(HttpResponse(entity = data))
    }

    "return true if all rows are valid" in {
      val callback = UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))))
      val data = "2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no"

      when(mockErsValidator.getCells(any(), any()))
        .thenReturn(Seq(Cell("A", 0, "2015-09-23"), Cell("B", 0, "250"),
          Cell("C", 0, "123.12"), Cell("D", 0, "12.1234"), Cell("E", 0, "12.1234"),
          Cell("F", 0, "no"), Cell("G", 0, "yes"),
          Cell("H", 0, "AB12345678"), Cell("I", 0, "no")
        ))
      val resultFuture = testProcessCsvService.processFiles(Some(callback), "csop", returnStubSource(_ , data))

      val result = resultFuture.map(_.futureValue)
      assert(result.forall(_.isRight))
      result shouldBe List(Right(true))
    }
    "return false when the data contains at least one invalid row" in {
      val callback = UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))))
      val data = "2015-09-23,test,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no"

      when(mockErsValidator.validateRow(any())(any(), any()))
        .thenReturn(Some(List(ValidationError(Cell("A", 0, "25  2015"), "error.1", "001", "ers.upload.error.date"))))
      when(mockErsValidator.getCells(any(), any()))
        .thenReturn(Seq(Cell("A", 0, "badValue$£")))
      val resultFuture = testProcessCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data))

      val result = resultFuture.map(_.futureValue)
      assert(result.forall(_.isRight))
      result shouldBe List(Right(false))
    }

    "return a throwable when an error occurs during the file validation" in {
      val callback = UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))))
      val data = ""
      val resultFuture = testProcessCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data))

      val result = resultFuture.map(_.futureValue)
      assert(result.forall(_.isLeft))
      result.head.left.value
        .getMessage shouldBe "The file that you chose doesn’t contain any data.<br/><br/>You won’t be able to upload CSOP_OptionsGranted_V4.csv as part of your annual return."
    }

    "return a throwable when an error occurs during the file processing" in {
      val callback = UpscanCsvFilesCallbackList(List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))))
      val data = "2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no"
      when(mockDataGenerator.setValidatorCsv(any())(any(), any())).thenReturn(Left(ERSFileProcessingException(
        "ers.exceptions.dataParser.configFailure",
        Messages("ers.exceptions.dataParser.validatorError"),
        optionalParams = Seq("test")
      )))

      val resultFuture = testProcessCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data))

      val result = resultFuture.map(_.futureValue)
      assert(result.forall(_.isLeft))
      result.head.left.value
        .getMessage shouldBe "ers.exceptions.dataParser.configFailure"
    }

  }

  "extractEntityData" should {

    "extract the data from an HttpResponse" in {
      val response = HttpResponse(entity = "Test response body")
      val resultFuture = testProcessCsvService.extractEntityData(response).runWith(Sink.head)

      val result = Await.result(resultFuture, Duration.Inf)
      result.utf8String shouldBe "Test response body"
    }

    "return a failed source with an UpstreamErrorResponse if response status is not Ok (200)" in {
      val response = HttpResponse(status = StatusCodes.InternalServerError)
      val resultFuture = testProcessCsvService.extractEntityData(response).runWith(Sink.head)

      assert(resultFuture.failed.futureValue.getMessage.contains("Illegal response from Upscan"))
    }
  }

  "extractBodyOfRequest" should {

    "extract the body from an Http response stream and read as csv" in {
      val source = Source
        .single(HttpResponse(entity = "0, 1, 2, 3\n4, 5"))

      val resultFuture = testProcessCsvService.extractBodyOfRequest(source).runWith(Sink.seq)
      val result = Await.result(resultFuture, Duration.Inf)

      assert(result.forall(_.isRight))
      result.head.value shouldBe List(ByteString("0"), ByteString(" 1"), ByteString(" 2"), ByteString(" 3"))
      result(1).value shouldBe List(ByteString("4"), ByteString(" 5"))
    }

    "return a left containing a throwable when an error occurs" in {
      val source = Source
        .single(HttpResponse(status = StatusCodes.InternalServerError))

      val resultFuture = testProcessCsvService.extractBodyOfRequest(source).runWith(Sink.seq)
      val result = Await.result(resultFuture, Duration.Inf)
      result.head.isLeft shouldBe true
      val exception = result.head.left.value
      assert(exception.isInstanceOf[UpstreamErrorResponse])
      assert(exception.asInstanceOf[UpstreamErrorResponse].getMessage().contains("Illegal response from Upscan"))
    }
  }

  "checkFileType" should {
    "check the file is a csv and remove the extension" in {
      val result = testProcessCsvService.checkFileType("test.csv")
      result.isRight shouldBe true
      result.value shouldBe "test"
    }

    "if the file is not a csv throw an ERSFileProcessingException" in {
      val result = testProcessCsvService.checkFileType("test.ods")
      result.isLeft shouldBe true
      result.left.value.getMessage shouldBe "You chose to check a CSV file, but test.ods isn’t a CSV file."
    }
  }

  "processDisplayedErrors" should {
    "correctly update the cells with the row number for as many errors as required" when {

      "asked to display 3 errors given 2 rows with 3 errors in each" in {
        val errors = Seq((List(
          ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0), (List(
          ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0)
        )
        val result = testProcessCsvService.processDisplayedErrors(3, errors)
        result.head shouldBe ((List(
          ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0))

        result(1) shouldBe ((List(
          ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0))
      }

      "asked to display 4 errors given 2 rows with 3 errors in each" in {
        val errors = Seq((List(
          ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0), (List(
          ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0)
        )
        val result = testProcessCsvService.processDisplayedErrors(4, errors)
        result shouldBe Seq((List(
          ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
        ), 0), (List(
          ValidationError(Cell("A", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 2, "test"), "001", "error.1", "ers.upload.error.date")
        ), 1)
        )
      }

      "asked to display 5 errors given 7 rows with 1 error in each" in {
        val errors = Seq(
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0)
        )

        val result = testProcessCsvService.processDisplayedErrors(5, errors)

        result shouldBe Seq(
          (List(ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 2, "test"), "001", "error.1", "ers.upload.error.date")), 1),
          (List(ValidationError(Cell("A", 3, "test"), "001", "error.1", "ers.upload.error.date")), 2),
          (List(ValidationError(Cell("A", 4, "test"), "001", "error.1", "ers.upload.error.date")), 3),
          (List(ValidationError(Cell("A", 5, "test"), "001", "error.1", "ers.upload.error.date")), 4),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0),
          (List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")), 0)
        )
      }
    }

    "return the original input when there are no errors" in {
      val errors = Seq(
        (List.empty[ValidationError], 0),
        (List.empty[ValidationError], 0),
        (List.empty[ValidationError], 0),
        (List.empty[ValidationError], 0)
      )
      val result = testProcessCsvService.processDisplayedErrors(3, errors)

      result shouldBe errors
    }
  }

  "giveRowNumbers" should {

    "return a seq of list of validation errors with correct row numbers" in {
      val errors = Seq(List(
        ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
      ))
      val result = testProcessCsvService.giveRowNumbers(errors)

      result shouldBe Seq(List(
        ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("B", 1, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
      ))
    }
  }

  "getRowsWithNumbers" should {

    "return validation errors if there are no exceptions" in {
      val errors = Seq(Right(RowValidationResults(List(
        ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
      ))))
      val result = testProcessCsvService.getRowsWithNumbers(errors, "test.csv")

      result.isRight shouldBe true
      result.map { value =>
        value shouldBe Seq(List(
          ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date")
        ))
      }
    }

    "return an exception if the file is empty" in {
      val errors = Seq.empty[Either[Throwable, RowValidationResults]]
      val result = testProcessCsvService.getRowsWithNumbers(errors, "test.csv")

      result.isLeft shouldBe true
      result.left.value
        .getMessage shouldBe "The file that you chose doesn’t contain any data.<br/><br/>You won’t be able to upload test.csv as part of your annual return."
    }

    "return the earliest previous exception if one exists" in {
      val errors = Seq(
        Left(ERSFileProcessingException("test error", "b")),
        Right(RowValidationResults(List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")))),
        Left(ERSFileProcessingException("a", "b"))
      )
      val result = testProcessCsvService.getRowsWithNumbers(errors, "test.csv")

      result.isLeft shouldBe true
      result.left.value.getMessage shouldBe "test error"
    }
  }

  "checkValidityOfRows" should {

    "return true if there are no validation errors in any row" in {
      val errors = Seq(List.empty[ValidationError], List.empty[ValidationError], List.empty[ValidationError])
      val callback = UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))

      val resultFuture = testProcessCsvService.checkValidityOfRows(errors, "CSOP_OptionsGranted_V4.csv", callback)

      val result = Await.result(resultFuture, Duration.Inf)

      result.isRight shouldBe true
      result.value shouldBe true
    }

    "return false and cache errors if there are validation errors in any row" in {
      when(mockErsUtil.SCHEME_ERROR_COUNT_CACHE).thenReturn("10")
      when(mockSessionCacheRepo.cache[Long](any(), any())(any(), any())).thenReturn(Future(("", "")))
      when(mockSessionCacheRepo.cache[ListBuffer[SheetErrors]](any(), any())(any(), any())).thenReturn(Future(("", "")))
      val errors = Seq(
        List(ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date")),
        List.empty[ValidationError],
        List(ValidationError(Cell("A", 3, "test"), "001", "error.1", "ers.upload.error.date"))
      )
      val callback = UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V4.csv", "no", Some(1)))

      val resultFuture = testProcessCsvService.checkValidityOfRows(errors, "CSOP_OptionsGranted_V4.csv", callback)

      val result = Await.result(resultFuture, Duration.Inf)

      result.isRight shouldBe true
      result.value shouldBe false
    }
  }
}
