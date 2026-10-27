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
import uk.gov.hmrc.validator.{DataEngine, SchemeVersion}
import uk.gov.hmrc.validator.models.{Cell, ValidationError}
import uk.gov.hmrc.validator.models.csv.RowValidationResults
import uk.gov.hmrc.validator.models.ods.SheetErrors

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class CsvErrorKeyTestCase(data: String, sheetName: String, expectedErrorKey: String)

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

  when(mockAppConfig.useV4andV5SchemeEnabledFrom).thenReturn(true)
  when(mockAppConfig.useV6andV7SchemeEnabledFrom).thenReturn(false)

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

    Seq(
      CsvErrorKeyTestCase(
        "2015,250,123.12,12.1234,12.1234,no,yes,AB12345678,no",
        "CSOP_OptionsGranted_V5",
        "ers.upload.error.date"
      ),
      CsvErrorKeyTestCase(
        "2015-07-23,,Daniel,Springer,AB123456A,123/CD1234,2015-08-29,123.12,yes,no,12.1234,12.1234,12.1234,yes,AB12345678,yes,yes,12.1234,yes,yes",
        "CSOP_OptionsExercised_V5",
        "ers.upload.error.first-name"
      ),
      CsvErrorKeyTestCase(
        "2015-07-23,Mark,AbcdefghijklmnopqrstuvwxyzAbcdefghijklmnopqrstuvwxyz,Springer,AB123456A,123/CD1234,2015-08-29,123.12,yes,no,12.1234,12.1234,12.1234,yes,AB12345678,yes,yes,12.1234,yes,yes",
        "CSOP_OptionsExercised_V5",
        "ers.upload.error.second-name"
      ),
      CsvErrorKeyTestCase(
        "2015-07-23,Mark,R,,AB123456A,123/CD1234,2015-08-29,123.12,yes,no,12.1234,12.1234,12.1234,yes,AB12345678,yes,yes,12.1234,yes,yes",
        "CSOP_OptionsExercised_V5",
        "ers.upload.error.last-name"
      ),
      CsvErrorKeyTestCase(
        "2015-10-09,yes,12.1234,Jonathan,Riley,Stinston,,123/CD1234,yes",
        "CSOP_OptionsRCL_V5",
        "ers.upload.error.nino"
      ),
      CsvErrorKeyTestCase(
        "2015-01-02,yes,12345678,John,Adam,Williams,AA123456A,123/AB12345678,2015-01-03,Company Name,12 Example Street,Example,Exampley,Example-upon-River,United Kingdom,SE1 2AB,AB123456,1234567890,123/XZ12345678910199,Another Company Name,12 Test Street,Test,Testing,Testing-upon-River,United Kingdom,TE123ST,AB123456,1234567890,123/DC12345678,yes,123.12,12.1234,12.1234,no,yes,AB12345678,12.1234,yes,12.1234,yes,yes,yes",
        "Non-TASS_Options_V7",
        "ers.upload.error.paye"
      ),
      CsvErrorKeyTestCase(
        "2015-10-09,yes,12.1234,Jonathan,Riley,Stinston,AB123456A,123/CD1234/1234,yes",
        "CSOP_OptionsRCL_V5",
        "ers.upload.error.paye-emp"
      ),
      CsvErrorKeyTestCase(
        "2015-07-23,Mark,Daniel,Springer,AB123456A,123/CD1234,2015-08-29,123.12,yes,no,12.1234,12.1234,12.1234,yes,,yes,yes,12.1234,yes,yes",
        "CSOP_OptionsExercised_V5",
        "ers.upload.error.hmrcRef"
      ),
      CsvErrorKeyTestCase(
        "2015-01-02,yes,12345678AA,John,Davey,Doe,AB123456A,123/BG12345678,123.12,12.1234,12.1234,12.1234,yes,yes",
        "Non-TASS_Sold_V7",
        "ers.upload.error.srn"
      ),
      CsvErrorKeyTestCase(
        "2015-03-24,100,4,yes,2:1,10.1234,10.12,10.1234,123456AA,123456,123456,123456,123456,123456,no,yes,AB12345678",
        "SIP_Awards_V4",
        "ers.upload.error.digitsUpTo2"
      ),
      CsvErrorKeyTestCase(
        "2015-01-01,250,123.12123,12.1234,12.1234,no,yes,AB12345678,no",
        "CSOP_OptionsGranted_V5",
        "ers.upload.error.digits2"
      ),
      CsvErrorKeyTestCase(
        "2015-01-03,yes,12345678,Julie,Erin,Smith,AB123456A,123/AB12345678,Company Name,12 Example Road,Example,Exampley,Example-upon-River,United Kingdom,TE123ST,AB123456,1234567890,123/AB12345678,1,yes,no,yes,12345678,123.12,1,2,1234567.12345,12.1234,1234.1234,yes,all,12.1234,12.1234,yes,yes,1,yes,yes,yes,no",
        "Non-TASS_Acquisition_V7",
        "ers.upload.error.digitsUpTo4"
      ),
      CsvErrorKeyTestCase(
        "2015-01-01,250,123.12,12.1234123,12.1234,no,yes,AB12345678,no",
        "CSOP_OptionsGranted_V5",
        "ers.upload.error.digits4"
      ),
      CsvErrorKeyTestCase(
        "2015-01-01,25000000,123.12,12.1234,12.1234,no,yes,AB12345678,no",
        "CSOP_OptionsGranted_V5",
        "ers.upload.error.int6"
      ),
      CsvErrorKeyTestCase(
        "2015-01-01,250,123.12,12.1234,12.1234,no,BLAH,AB12345678,no",
        "CSOP_OptionsGranted_V5",
        "ers.upload.error.yes-no"
      ),
      CsvErrorKeyTestCase(
        "2015-10-09,yes,12.1234,Jonathan,Riley,Stinston,AB123456A,123/CD1234,",
        "CSOP_OptionsRCL_V5",
        "ers.upload.error.paye-yes-no"
      ),
      CsvErrorKeyTestCase(
        "2015-01-03,yes,12345678,Julie,Erin,Smith,AB123456A,123/AB12345678,Company Name,12 ExampleRoad,Example,Exampley,Example-upon-River,United Kingdom,TE123ST,AB123456,1234567890,123/AB12345678,1,yes,no,yes,12345678,123.12,1,2,0.6,12.1234,12.1234,yes,all,12.1234,12.1234,yes,yes,,yes,yes,yes,no",
        "Non-TASS_Acquisition_V7",
        "ers.upload.error.enum1-3"
      ),
      CsvErrorKeyTestCase(
        "2015-03-24,100,6,yes,2:1,10.1234,10.12,10.1234,123456,123456,123456,123456,123456,123456, no,yes,AB12345678",
        "SIP_Awards_V4",
        "ers.upload.error.enum1-4"
      ),
      CsvErrorKeyTestCase(
        "no,no,yes,9,2015-12-09,John,Barry,Doe,AA123456A,123/XZ55555555,10.1234,100.12,10.1234,10.1234",
        "EMI40_Adjustments_V7",
        "ers.upload.error.enum1-8"
      ),
      CsvErrorKeyTestCase(
        "2015-01-03,yes,12345678,Julie,Erin,Smith,AB123456A,123/AB12345678,Company Name,12 Example Road,Example,Exampley,Example-upon-River,United Kingdom,TE123ST,AB123456,1234567890,123/AB12345678,10,yes,no,yes,12345678,123.12,1,2,0.6,12.1234,12.1234,yes,all,12.1234,12.1234,yes,yes,1,yes,yes,yes,no",
        "Non-TASS_Acquisition_V7",
        "ers.upload.error.enum1-9"
      ),
      CsvErrorKeyTestCase(
        "2015-01-03,yes,12345678,Julie,Erin,Smith,AB123456A,123/AB12345678,Company Name,12 Example Road,Example,Exampley,Example-upon-River,United Kingdom,TE123ST,AB123456,1234567890,123/AB12345678,1,yes,no,yes,12345678,123.12,1,2,0.6,12.1234,12.1234,yes,same,12.1234,12.1234,yes,yes,1,yes,yes,yes,no",
        "Non-TASS_Acquisition_V7",
        "ers.upload.error.enum-all-some"
      ),
      CsvErrorKeyTestCase(
        "2015-03-24,100,4,yes,2   1,10.1234,10.12,10.1234,123456,123456,123456,123456,123456,123456,no,yes,AB12345678",
        "SIP_Awards_V4",
        "ers.upload.error.ratio"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,,1 Example Street,Bucknall,Stoke,Staffordshire,United Kingdom,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.company-name"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,,Bucknall,Stoke,Staffordshire,United Kingdom,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-line1"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa,Stoke,Staffordshire,United Kingdom,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-line2"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa,Staffordshire,United Kingdom,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-line3"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,Stoke,aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa,United Kingdom,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-line4"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,Stoke,Staffordshire,aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa,SE1 2AB,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-country"
      ),
      CsvErrorKeyTestCase(
        "2015-01-03,yes,12345678,Julie,Erin,Smith,AB123456A,123/AB12345678,Company Name,12 Example Road,Example,Exampley,Example-upon-River,United Kingdom,TE123STABCD,AB123456,1234567890,123/AB12345678,1,yes,no,yes,12345678,123.12,1,2,0.6,12.1234,12.1234,yes,all,12.1234,12.1234,yes,yes,1,yes,yes,yes,no",
        "Non-TASS_Acquisition_V7",
        "ers.upload.error.address-postcode"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,Stoke,Staffordshire,United Kingdom,abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefgh,1234567890,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.address-postcode-mand"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,Stoke,Staffordshire,United Kingdom,SE1 2AB,1234567890,AB12345678010",
        "EMI40_Replaced_V4",
        "ers.upload.error.crn"
      ),
      CsvErrorKeyTestCase(
        "2015-12-09,2015-12-09,John,Barry,Smith,AA123456A,123/XZ55555555,10.1234,10,1 Example Street,Bucknall,Stoke,Staffordshire,United Kingdom,SE1 2AB,12345678901,AB123456",
        "EMI40_Replaced_V4",
        "ers.upload.error.ctr"
      )
    ).foreach { csvErrorKeyTestCase: CsvErrorKeyTestCase =>
      s"return ${csvErrorKeyTestCase.expectedErrorKey} when parsed data containing that error" in {
        val dataEngine = DataEngine(csvErrorKeyTestCase.sheetName, SchemeVersion.All).toOption.get
        Await
          .result(
            processCsvService.validateCsv(Source.single(HttpResponse(entity = csvErrorKeyTestCase.data)), dataEngine),
            Duration.Inf
          )
          .map { (validationResults: Seq[RowValidationResults]) =>
            val validationErrors: Seq[ValidationError] = validationResults.flatMap(_.validationErrors)
            validationErrors.length        shouldBe 1
            validationErrors.head.errorMsg shouldBe csvErrorKeyTestCase.expectedErrorKey
          }
      }
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
