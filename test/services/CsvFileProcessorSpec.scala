/*
 * Copyright 2018 HM Revenue & Customs
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
import models.ERSFileProcessingException
import org.scalatest.concurrent.Timeouts
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Span}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import services.validation.ErsValidator
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{Cell, DataValidator, ValidationError}
import play.api.i18n.Messages.Implicits._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.HeaderCarrier

class CsvFileProcessorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with Timeouts {

  // This ScalaStyle check prevents "actual should be a [Type]" syntax
  // scalastyle:off no.whitespace.before.left.bracket

  private val nl: String = System.lineSeparator()
  private val timeout = 100
  private val awaitTimeout = Span(timeout, Millis)

  val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
  if(fileCopied.exists)
    {
      fileCopied.getCanonicalFile.delete()
      fileCopied.getAbsoluteFile.delete()
    }

  val data =  Iterator(Array("25 Jan 2015","1234567","10.1","10.123"))
  val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Grants_V3.csv")
  Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)

  private def getMockFileCSV = {
    val userDirectory = System.getProperty("user.dir")
    val fileTest = new File(System.getProperty("user.dir") + "/test/resources/test1.csv")
    Files.copy(fileTest.toPath, new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/test2.csv").toPath)
    val tempFile = TemporaryFile(new java.io.File(userDirectory + "/test/resources/copy/test2.csv"))
    val part = FilePart[TemporaryFile](
      key = "fileParam",
      filename = "Other_Grants_V3.csv",
      contentType = Some("Content-Type: multipart/form-data"),
      ref = tempFile)
    val file = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq())
    file
  }

  private def createTempFile(content: String): File = {
    val file = File.createTempFile("test", "csv")
    val writer = new PrintWriter(file)
    writer.write(content)
    writer.close()
    file
  }

  "The CsvFileProcessor service " must {

    "validate file data and return errors" in {
      val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
      val dataValidator = DataValidator(ConfigFactory.load.getConfig("ers-other-grants-validation-config"))
      val result = CsvFileProcessor.validateFile(fileCopied, "Other_Grants_V3.csv", ErsValidator.validateRow(dataValidator))
    //  Thread.sleep(2000)
      result.size shouldBe 4
    }
    "read csv file" in {
      Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)
      val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
      val request = Fixtures.buildFakeRequestWithSessionId("POST")
      val result = CsvFileProcessor.readCSVFile("Other_Grants_V3", fileCopied, "3")(request, hc = HeaderCarrier())
      result.errors.size shouldBe  4

    }

    "validate multiple CSV files" in {
      val request = Fixtures.buildFakeRequestWithSessionId("POST").withMultipartFormDataBody(getMockFileCSV)
      val result = CsvFileProcessor.validateCsvFiles("3")(request, Fixtures.buildFakeUser,hc = HeaderCarrier())

      val expected = getMockFileCSV.files.size
      result.size shouldEqual expected
    }

    "throw the correct error message in the validateFile function" in{
      val result = intercept[Exception]{
        def validator(rowData:Seq[String],rowCount:Int): Option[List[ValidationError]] = throw new Exception
        Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)
        val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
        CsvFileProcessor.validateFile(fileCopied, "Other_Grants_V3.csv", validator)
      }
      result.getMessage shouldEqual  Messages("ers.exceptions.dataParser.fileParsingError", "Other_Grants_V3.csv")
    }

    "throw correct exception if an empty csv is given" in {
      val result = intercept[ERSFileProcessingException] {
        val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Acquisition_V3.csv")
        Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Acquisition_V3.csv").toPath)
        val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Acquisition_V3.csv")
        val dataValidator = DataValidator(ConfigFactory.load.getConfig("ers-other-acquisition-validation-config"))
        CsvFileProcessor.validateFile(fileCopied,"Other_Acquisition_V3",ErsValidator.validateRow(dataValidator))
      }
      result.getMessage shouldEqual Messages("ers_check_csv_file.noData", "Other_Acquisition_V3.csv")
    }

  }

  "converter should split by comma" in {
    CsvFileProcessor.converter("a,b,c") shouldBe Array("a","b","c")
  }

  "checkRowsExist" must {
    "return true if the number of rows found is greater than zero" in {
      CsvFileProcessor.checkRowsExist(1, "test") shouldEqual Success(true)
    }

    "return an exception if the number of rows is not greater than zero" in {
      val actual = CsvFileProcessor.checkRowsExist(0, "test")
      actual shouldBe a [Failure[_]]
      val throwable = actual.failed.get
      throwable shouldBe a [ERSFileProcessingException]
      throwable.getMessage shouldEqual Messages("ers_check_csv_file.noData", "test.csv")
    }
  }

  private def getRowsFromFileFixture = new {
    val content = s"field1, field2, field3${nl}field1${nl}field1, field2"
    val sheetName = "Other_Options_V3.csv"
  }

  "getRowsFromFile" must {

    "return the correct number of rows" in {
      val fixture = getRowsFromFileFixture
      val file = createTempFile(fixture.content)
      val (_, actual) = CsvFileProcessor.getRowsFromFile(file, fixture.sheetName)
      actual shouldEqual 3
    }

    "return the rows from the file" in {
      val fixture = getRowsFromFileFixture
      val file = createTempFile(fixture.content)
      val (actual, _) = CsvFileProcessor.getRowsFromFile(file, fixture.sheetName)
      val expected: List[List[String]] = fixture.content.split(nl).toList.map(s => s.split(",").toList)
      actual shouldEqual expected
    }

    "handle empty files" in {
      val fixture = getRowsFromFileFixture
      val file = createTempFile("")
      val actual = CsvFileProcessor.getRowsFromFile(file, fixture.sheetName)
      actual shouldEqual (Nil, 0)
    }
  }

  "numberOfChunks method" must {
    "return the correct number of chunks" in {
      // scalastyle:off magic.number
      CsvFileProcessor.numberOfChunks(0, 1) shouldEqual 0
      CsvFileProcessor.numberOfChunks(1, 2) shouldEqual 1
      CsvFileProcessor.numberOfChunks(2, 2) shouldEqual 1
      CsvFileProcessor.numberOfChunks(9, 2) shouldEqual 5
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

      val actual = CsvFileProcessor.submitChunks(fixture.rows, 3, 2, fixture.sheetName, dummyValidator)

      actual.length shouldEqual 3
    }

    "process all rows" in {
      val fixture = submitChunksFixture
      var count = new AtomicInteger(0)
      val dummyValidator: (Seq[String], Int) => Option[List[ValidationError]] = (_, _) => {
        count.incrementAndGet()
        Some(Nil)
      }

      val actual = CsvFileProcessor.submitChunks(fixture.rows, 3, 2, fixture.sheetName, dummyValidator)

      failAfter(awaitTimeout) {
        Await.ready(actual, Duration.Inf)
      }
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

      CsvFileProcessor.processChunk(fixture.chunk, 1, fixture.sheetName, dummyValidator)
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

      val actual = CsvFileProcessor.processChunk(fixture.chunk, 1, fixture.sheetName, dummyValidator)
      actual shouldEqual expected
    }
  }

  "getResult" must {
    "return an empty list when there are no errors" in {
      val submissions:Array[Future[List[ValidationError]]] = Array(
        Future(List.empty[ValidationError]),
        Future(List.empty[ValidationError])
      )

      val actual = CsvFileProcessor.getResult(submissions)
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

      val actual = CsvFileProcessor.getResult(submissions)
      failAfter(awaitTimeout) {
        Await.ready(actual, Duration.Inf)
      }

      actual.value shouldEqual expected
    }
  }

}
