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

import helpers.ErsTestHelper
import models.upscan._
import models.ERSFileProcessingException
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.testkit.TestKit
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesImpl
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.DefaultMessagesControllerComponents
import play.api.{Application, i18n}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.csv.RowValidationResults
import uk.gov.hmrc.validator.models.ods.SheetErrors

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
    with ScalaFutures {

  override lazy val fakeApplication: Application = new GuiceApplicationBuilder()
    .configure(
      Map(
        "play.i18n.langs" -> List("en", "cy"),
        "metrics.enabled" -> "false"
      )
    )
    .build()

  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication)
  implicit lazy val testMessages: MessagesImpl      = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  val processCsvService: ProcessCsvService = new ProcessCsvService(mockAppConfig, mockSessionCacheRepo, mockErsUtil)

  when(mockAppConfig.csopV5Enabled).thenReturn(true)

  "extractBodyOfRequest" should {

    "extract the body from an Http response stream and read as csv" in {
      val source = Source
        .single(HttpResponse(entity = "0, 1, 2, 3\n4, 5"))

      val resultFuture: Future[Seq[Either[Throwable, List[ByteString]]]] =
        processCsvService.extractBodyOfRequest(source).runWith(Sink.seq)
      val result: Seq[Either[Throwable, List[ByteString]]]               = Await.result(resultFuture, Duration.Inf)

      assert(result.forall(_.isRight))
      result.head.map(_ shouldBe List(ByteString("0"), ByteString(" 1"), ByteString(" 2"), ByteString(" 3")))
      result(1).map(_ shouldBe List(ByteString("4"), ByteString(" 5")))
    }

    "return a left containing a throwable when an error occurs" in {
      val source = Source
        .single(HttpResponse(status = StatusCodes.InternalServerError))

      val resultFuture = processCsvService.extractBodyOfRequest(source).runWith(Sink.seq)
      val result       = Await.result(resultFuture, Duration.Inf)
      result.head.isLeft shouldBe true
      val exception: Either[List[ByteString], Throwable] = result.head.swap
      exception.map { e: Throwable =>
        assert(e.isInstanceOf[UpstreamErrorResponse])
        assert(e.asInstanceOf[UpstreamErrorResponse].getMessage.contains("Illegal response from Upscan"))
      }
    }
  }

  "processFiles" should {

    when(mockErsUtil.SCHEME_ERROR_COUNT_CACHE).thenReturn("10")
    when(mockSessionCacheRepo.cache[Any](any(), any())(any(), any())).thenReturn(Future(("", "")))

    def returnStubSource(x: String, data: String): Source[HttpResponse, NotUsed] =
      Source.single(HttpResponse(entity = data))

    "return a Right containing true if row is valid" in {
      val callback     = UpscanCsvFilesCallbackList(
        List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V5.csv", "no", Some(1))))
      )
      val data: String = "2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no"

      processCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data)).foreach { futureResult =>
        val result: Either[Throwable, Boolean] = Await.result(futureResult, Duration.Inf)
        assert(result.isRight)
        result.map(validFile => assert(validFile))
      }
    }

    "return false when the data contains at least one invalid row" in {
      val callback = UpscanCsvFilesCallbackList(
        List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V5.csv", "no", Some(1))))
      )
      val data     =
        "2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,test,123.12,12.1234,12.1234,no,yes,AB12345678,no\n"

      processCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data)).foreach { futureResult =>
        val result: Either[Throwable, Boolean] = Await.result(futureResult, Duration.Inf)
        assert(result.isRight)
        result.map(validFile => assert(!validFile))
      }
    }

    "return a throwable when an error occurs during the file validation" in {
      val callback     = UpscanCsvFilesCallbackList(
        List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V5.csv", "no", Some(1))))
      )
      val data         = ""
      val resultFuture = processCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data))

      val result = resultFuture.map(_.futureValue)
      assert(result.forall(_.isLeft))
      val value  = result.head.left.toOption.get.asInstanceOf[ERSFileProcessingException]

      value.context shouldBe "The file that you chose doesn’t contain any data.<br/><br/>You won’t be able to upload CSOP_OptionsGranted_V5 as part of your annual return."
    }

    "return a throwable when an error occurs during the file processing" in {
      val callback             = UpscanCsvFilesCallbackList(
        List(UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("NOT A VALID SHEET NAME", "no", Some(1))))
      )
      val data                 =
        "2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no\n2015-09-23,250,123.12,12.1234,12.1234,no,yes,AB12345678,no"
      val exception: Throwable =
        processCsvService.processFiles(Some(callback), "csop", returnStubSource(_, data)).head.futureValue.swap.value
      exception.getMessage shouldBe "You chose to check a CSV file, but NOT A VALID SHEET NAME isn’t a CSV file."
    }
  }

  "extractEntityData" should {

    "extract the data from an HttpResponse" in {
      val response     = HttpResponse(entity = "Test response body")
      val resultFuture = processCsvService.extractEntityData(response).runWith(Sink.head)

      val result = Await.result(resultFuture, Duration.Inf)
      result.utf8String shouldBe "Test response body"
    }

    "return a failed source with an UpstreamErrorResponse if response status is not Ok (200)" in {
      val response     = HttpResponse(status = StatusCodes.InternalServerError)
      val resultFuture = processCsvService.extractEntityData(response).runWith(Sink.head)

      assert(resultFuture.failed.futureValue.getMessage.contains("Illegal response from Upscan"))
    }
  }

  "checkIfValidationResultsEmpty" should {

    "return true if there is one RowValidationResults record which is empty" in {
      val emptyValidationResults = Seq(
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true)
      )
      assert(processCsvService.checkIfValidationResultsEmpty(emptyValidationResults))
    }

    "return true if there are multiple RowValidationResults record which are all empty" in {
      val emptyValidationResults = Seq(
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true),
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true),
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true),
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true)
      )
      assert(processCsvService.checkIfValidationResultsEmpty(emptyValidationResults))
    }

    "return false if there are multiple RowValidationResults record with any which are not empty" in {
      val emptyValidationResults = Seq(
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true),
        RowValidationResults(
          List(ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")),
          rowWasEmpty = false
        ),
        RowValidationResults(List.empty[ValidationError], rowWasEmpty = true)
      )
      assert(!processCsvService.checkIfValidationResultsEmpty(emptyValidationResults))
    }

  }

  "getValidationResultsWithCorrectRowNumber" should {

    "return validation errors if there are no exceptions" in {
      val errors = Seq(
        RowValidationResults(
          List(
            ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
            ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
            ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
          )
        ),
        RowValidationResults(
          List(
            ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
            ValidationError(Cell("B", 0, "test"), "001", "error.1", "ers.upload.error.date"),
            ValidationError(Cell("C", 0, "test"), "001", "error.1", "ers.upload.error.date")
          )
        ),
        RowValidationResults(
          List(
            ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date")
          )
        )
      )
      val result = processCsvService.getValidationResultsWithCorrectRowNumber(errors, "test.csv")

      result.isRight shouldBe true
      result.map { value: Seq[ValidationError] =>
        value should contain theSameElementsAs Seq(
          ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 1, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("A", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("B", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("C", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("A", 3, "test"), "001", "error.1", "ers.upload.error.date")
        )
      }
    }

    "return an exception if the file is empty" in {
      val errors = Seq.empty[RowValidationResults]
      val result = processCsvService.getValidationResultsWithCorrectRowNumber(errors, "test.csv")
      val value  = result.left.toOption.get.asInstanceOf[ERSFileProcessingException]

      result.isLeft shouldBe true
      value.context shouldBe "The file that you chose doesn’t contain any data.<br/><br/>You won’t be able to upload test.csv as part of your annual return."
    }
  }

  "updateValidationResultRowNumbers" should {

    "return an empty sequence when parsed a sequence of RowValidationResults which contain no errors" in {
      val validationResultsWithoutIndex = Seq(
        RowValidationResults(validationErrors = List.empty[ValidationError]),
        RowValidationResults(validationErrors = List.empty[ValidationError]),
        RowValidationResults(validationErrors = List.empty[ValidationError]),
        RowValidationResults(validationErrors = List.empty[ValidationError])
      )
      processCsvService.updateValidationResultRowNumbers(validationResultsWithoutIndex) shouldBe Seq
        .empty[ValidationError]
    }

    "return a list of ValidationErrors with the correct row numbers when parsed a list of validation results " +
      "containing validation errors" in {
        val validationResultsWithIndex = Seq(
          RowValidationResults(validationErrors = List.empty[ValidationError]),
          RowValidationResults(validationErrors =
            List(
              ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
              ValidationError(Cell("I", 0, "noooo"), "009", "error.9", "Enter 'yes' or 'no'")
            )
          ),
          RowValidationResults(validationErrors =
            List(
              ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
              ValidationError(Cell("I", 0, "noooo"), "009", "error.9", "Enter 'yes' or 'no'")
            )
          ),
          RowValidationResults(validationErrors = List.empty[ValidationError]),
          RowValidationResults(validationErrors =
            List(
              ValidationError(Cell("A", 0, "test"), "001", "error.1", "ers.upload.error.date"),
              ValidationError(Cell("I", 0, "noooo"), "009", "error.9", "Enter 'yes' or 'no'")
            )
          )
        )
        val expectedValidationErrors   = Seq(
          ValidationError(Cell("A", 2, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("I", 2, "noooo"), "009", "error.9", "Enter 'yes' or 'no'"),
          ValidationError(Cell("A", 3, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("I", 3, "noooo"), "009", "error.9", "Enter 'yes' or 'no'"),
          ValidationError(Cell("A", 5, "test"), "001", "error.1", "ers.upload.error.date"),
          ValidationError(Cell("I", 5, "noooo"), "009", "error.9", "Enter 'yes' or 'no'")
        )
        processCsvService.updateValidationResultRowNumbers(
          validationResultsWithIndex
        ) should contain theSameElementsAs expectedValidationErrors
      }

  }

  "checkValidityOfRows" should {

    "return true if there are no validation errors in any row" in {
      val callback =
        UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V5.csv", "no", Some(1)))

      val resultFuture =
        processCsvService.checkValidityOfRows(List.empty[ValidationError], "CSOP_OptionsGranted_V5.csv", callback)

      val result = Await.result(resultFuture, Duration.Inf)

      result.isRight shouldBe true
      result.value   shouldBe true
    }

    "return false and cache errors if there are validation errors in any row" in {
      when(mockErsUtil.SCHEME_ERROR_COUNT_CACHE).thenReturn("10")
      when(mockSessionCacheRepo.cache[Long](any(), any())(any(), any())).thenReturn(Future(("", "")))
      when(mockSessionCacheRepo.cache[ListBuffer[SheetErrors]](any(), any())(any(), any())).thenReturn(Future(("", "")))
      val errors   = Seq(
        ValidationError(Cell("A", 1, "test"), "001", "error.1", "ers.upload.error.date"),
        ValidationError(Cell("A", 3, "test"), "001", "error.1", "ers.upload.error.date")
      )
      val callback =
        UpscanCsvFilesCallback(UploadId("1"), UploadedSuccessfully("CSOP_OptionsGranted_V5.csv", "no", Some(1)))

      val resultFuture = processCsvService.checkValidityOfRows(errors, "CSOP_OptionsGranted_V5.csv", callback)

      val result = Await.result(resultFuture, Duration.Inf)

      result.isRight shouldBe true
      result.value   shouldBe false
    }
  }

}
