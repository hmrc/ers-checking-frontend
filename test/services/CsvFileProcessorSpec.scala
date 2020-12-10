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

package services

import java.io.{File, PrintWriter}
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

import com.typesafe.config.ConfigFactory
import controllers.Fixtures
import helpers.ErsTestHelper
import models.ERSFileProcessingException
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{ScalaFutures, TimeLimits}
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n
import play.api.i18n.{Messages, MessagesImpl}
import play.api.mvc.DefaultMessagesControllerComponents
import services.validation.ErsValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{Cell, DataValidator, ValidationError}
import utils.{ParserUtil, UploadedFileUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.BufferedSource
import scala.util.{Failure, Success}

class CsvFileProcessorSpec extends UnitSpec with ErsTestHelper with GuiceOneAppPerSuite with TimeLimits with ScalaFutures {

  private val nl: String = System.lineSeparator()
  private val timeout = 100
  private val awaitTimeout = Span(timeout, Millis)

  val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
  if(fileCopied.exists)
    {
      fileCopied.getCanonicalFile.delete()
      fileCopied.getAbsoluteFile.delete()
    }

  val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Grants_V3.csv")
  Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)

  def convertToBufferedSource(file: File): BufferedSource = {
    import scala.io.Source._
    fromFile(file)
  }

  private def createTempFile(content: String): File = {
    val file = File.createTempFile("test", "csv")
    val writer = new PrintWriter(file)
    writer.write(content)
    writer.close()
    file
  }

  val mockUploadedFileUtil: UploadedFileUtil = mock[UploadedFileUtil]
  lazy val testParserUtil: ParserUtil = fakeApplication.injector.instanceOf[ParserUtil]
  val mockDataGenerator: DataGenerator = mock[DataGenerator]
  lazy val mcc: DefaultMessagesControllerComponents = testMCC(fakeApplication())
  implicit lazy val testMessages: MessagesImpl = MessagesImpl(i18n.Lang("en"), mcc.messagesApi)

  def testCsvFileProcessor: CsvFileProcessor = new CsvFileProcessor(mockDataGenerator, testParserUtil, mockUploadedFileUtil, mockAppConfig)

  "The CsvFileProcessor service " must {

    "validate file data and return errors" in {
      when(mockDataGenerator.isBlankRow(any())).thenReturn(false)
      val source = convertToBufferedSource(new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv"))
      val fileCopied = source.getLines()
      val dataValidator = DataValidator(ConfigFactory.load.getConfig("ers-other-grants-validation-config"))
      val result = testCsvFileProcessor.validateFile(fileCopied, "Other_Grants_V3.csv", ErsValidator.validateRow(dataValidator))
      source.close()
      result.size shouldBe 4
    }

    "read csv file" in {
      lazy val testValidator: DataValidator = ERSValidationConfigs.getValidator("ers-other-grants-validation-config")
      when(mockDataGenerator.identifyAndDefineSheet(any(), any())(any(), any(), any())).thenReturn("Other_Grants_V3")
      when(mockDataGenerator.setValidator(ArgumentMatchers.eq("Other_Grants_V3"))(any(), any(), any())).thenReturn(testValidator)
      when(mockDataGenerator.isBlankRow(any())).thenReturn(false)

      val source = convertToBufferedSource(new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv"))
      val fileCopied = source.getLines()
      val request = Fixtures.buildFakeRequestWithSessionId("POST")
      val result = testCsvFileProcessor.readCSVFile("Other_Grants_V3", fileCopied, "other")(request, hc = HeaderCarrier(), implicitly[Messages])
      source.close()
      result.errors.size shouldBe  4
    }

    "throw the correct error message in the validateFile function" in{
      val result = intercept[Exception]{
        def validator(rowData:Seq[String],rowCount:Int): Option[List[ValidationError]] = throw new Exception
        val source = convertToBufferedSource(new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv"))
        val fileCopied = source.getLines()
        testCsvFileProcessor.validateFile(fileCopied, "Other_Grants_V3.csv", validator)
        source.close()
      }
      result.getMessage shouldEqual  Messages("ers.exceptions.dataParser.fileParsingError", "Other_Grants_V3.csv")
    }

    "throw correct exception if an empty csv is given" in {
      val result = intercept[ERSFileProcessingException] {
        val source = convertToBufferedSource(new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Acquisition_V3.csv"))
        val fileCopied = source.getLines()
        val dataValidator = DataValidator(ConfigFactory.load.getConfig("ers-other-acquisition-validation-config"))
        testCsvFileProcessor.validateFile(fileCopied,"Other_Acquisition_V3", ErsValidator.validateRow(dataValidator))
        source.close()
      }
      result.getMessage shouldEqual Messages("ers_check_csv_file.noData", "Other_Acquisition_V3.csv")
    }

  }

  "converter should split by comma" in {
    testCsvFileProcessor.converter("a,b,c") shouldBe Array("a","b","c")
  }

  "checkRowsExist" must {
    "return true if the number of rows found is greater than zero" in {
      testCsvFileProcessor.checkRowsExist(1, "test") shouldEqual Success(true)
    }

    "return an exception if the number of rows is not greater than zero" in {
      val actual = testCsvFileProcessor.checkRowsExist(0, "test")
      actual shouldBe a[Failure[_]]
      val throwable = actual.failed.get
      throwable shouldBe a[ERSFileProcessingException]
      throwable.getMessage shouldEqual Messages("ers_check_csv_file.noData", "test.csv")
    }
  }

  private def getRowsFromFileFixture = new {
    val content = s"field1, field2, field3${nl}field1${nl}field1, field2"
  }

  "getRowsFromFile" must {

    "return the correct number of rows" in {
      val fixture = getRowsFromFileFixture
      val source = convertToBufferedSource(createTempFile(fixture.content))
      val file = source.getLines()
      val (_, actual) = testCsvFileProcessor.getRowsFromFile(file)
      source.close()
      actual shouldEqual 3
    }

    "return the rows from the file" in {
      val fixture = getRowsFromFileFixture
      val source = convertToBufferedSource(createTempFile(fixture.content))
      val file = source.getLines()
      val (actual, _) = testCsvFileProcessor.getRowsFromFile(file)
      val expected: List[List[String]] = fixture.content.split(nl).toList.map(s => s.split(",").toList)
      source.close()
      actual shouldEqual expected
    }

    "handle empty files" in {
      val source = convertToBufferedSource(createTempFile(""))
      val file = source.getLines()
      val actual = testCsvFileProcessor.getRowsFromFile(file)
      source.close()
      actual shouldEqual (Nil, 0)
    }
  }

  "numberOfChunks method" must {
    "return the correct number of chunks" in {
      // scalastyle:off magic.number
      testCsvFileProcessor.numberOfChunks(0, 1) shouldEqual 0
      testCsvFileProcessor.numberOfChunks(1, 2) shouldEqual 1
      testCsvFileProcessor.numberOfChunks(2, 2) shouldEqual 1
      testCsvFileProcessor.numberOfChunks(9, 2) shouldEqual 5
      // scalastyle:on magic.number
    }
  }

  private def submitChunksFixture = new {
    val rows = List(
      List("field1", "field2"),
      List("field1", "field2"),
      List("field1", "field2"),
      List("field1", "field2"),
      List("field1", "field2")
    )
    val sheetName = "Other_Options_V3.csv"
  }

  "submitChunks" must {
    "submit the correct number of futures" in {
      val fixture = submitChunksFixture
      val dummyValidator: (Seq[String], Int) => Option[List[ValidationError]] = (_, _) => {
        Some(Nil)
      }

      val actual = testCsvFileProcessor.submitChunks(fixture.rows, 3, 2, fixture.sheetName, dummyValidator)

      actual.length shouldEqual 3
    }


      "process all rows" in {


        val fixture = submitChunksFixture
        var count = new AtomicInteger(0)
        val dummyValidator: (Seq[String], Int) => Option[List[ValidationError]] = (_, _) => {
          count.incrementAndGet()
          Some(Nil)
        }

        val actual: Array[Future[List[ValidationError]]] = testCsvFileProcessor.submitChunks(fixture.rows, 3, 2, fixture.sheetName, dummyValidator)

        Future.sequence(actual.toList).futureValue

        count.get() shouldEqual 5
      }
    }


  private def processChunkFixture = new {
    val chunk = List(
      List("field1", "field2"),
      List("field1", "field2"),
      List("field1", "field2")
    )
    val sheetName = "Other_Options_V3.csv"
  }

  "processChunk" must {
    "process each row in the chunk" in {
      val fixture = processChunkFixture
      var count: Int = 0
      val dummyValidator: (Seq[String], Int) => Option[List[ValidationError]] = (_, _) => {
        count += 1
        Some(Nil)
      }

      testCsvFileProcessor.processChunk(fixture.chunk, 1, fixture.sheetName, dummyValidator)
      count shouldEqual fixture.chunk.size
    }

    "return the correct errors" in {
      val fixture = processChunkFixture
      val expected: ListBuffer[ValidationError] = new ListBuffer()
      val dummyValidator: (Seq[String], Int) => Option[List[ValidationError]] = (_, rowNo) => {
        if (rowNo == 1) {
          val errors = List(ValidationError(Cell("AA", rowNo, "bad"), "bad data", "bad cell AA", "cell AA is bad"))
          expected ++= errors
          Some(errors)
        }
        else if (rowNo == 2) {
          Some(Nil)
        }
        else {
          val errors = List(
            ValidationError(Cell("AA", rowNo, "bad"), "bad data", "bad cell AA", "cell AA is bad"),
            ValidationError(Cell("AB", rowNo, "bad"), "bad data", "bad cell AB", "cell AB is bad")
          )
          expected ++= errors
          Some(errors)
        }
      }

      val actual = testCsvFileProcessor.processChunk(fixture.chunk, 1, fixture.sheetName, dummyValidator)
      actual shouldEqual expected
    }
  }

  "getResult" must {
    "return an empty list when there are no errors" in {
      val submissions:Array[Future[List[ValidationError]]] = Array(
        Future(List.empty[ValidationError]),
        Future(List.empty[ValidationError])
      )

      val actual = testCsvFileProcessor.getResult(submissions)
      failAfter(awaitTimeout) {
        Await.ready(actual, Duration.Inf)
      }

      actual.value shouldEqual Some(Success(List.empty[ValidationError]))
    }

    "return the correct errors" in {
      val submissions:Array[Future[List[ValidationError]]] = Array(
        Future(List(ValidationError(Cell("AA", 1, "bad"), "bad data", "bad cell AA", "cell AA is bad"))),
        Future(List.empty[ValidationError]),
        Future(List(
          ValidationError(Cell("AA", 2, "bad"), "bad data", "bad cell AA", "cell AA is bad"),
          ValidationError(Cell("AA", 3, "bad"), "bad data", "bad cell AA", "cell AA is bad")
        ))
      )

      val expected = Some(Success(submissions.toList.flatten))

      val actual = testCsvFileProcessor.getResult(submissions)
      failAfter(awaitTimeout) {
        Await.ready(actual, Duration.Inf)
      }

      actual.value shouldEqual expected
    }
  }

}
