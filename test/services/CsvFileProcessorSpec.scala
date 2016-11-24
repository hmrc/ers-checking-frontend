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

package services

import java.io.File
import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import controllers.Fixtures
import models.ERSFileProcessingException
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import services.validation.ErsValidator
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}
import play.api.i18n.Messages.Implicits._

class CsvFileProcessorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite{

  implicit val messages = applicationMessages

  val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
  if(fileCopied.exists)
    {
      fileCopied.getCanonicalFile.delete()
      fileCopied.getAbsoluteFile.delete()
    }

  val data =  Iterator(Array("25 Jan 2015","1234567","10.1","10.123"))
  val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Grants_V3.csv")
  Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)

  def getMockFileCSV = {
    val userDirectory = System.getProperty("user.dir")
    val fileTest = new File(System.getProperty("user.dir") + "/test/resources/test1.csv")
    Files.copy(fileTest.toPath, new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/test2.csv").toPath)
    val tempFile = TemporaryFile(new java.io.File(userDirectory+"/test/resources/copy/test2.csv"))
    val part = FilePart[TemporaryFile](key = "fileParam", filename = "Other_Grants_V3.csv", contentType = Some("Content-Type: multipart/form-data"), ref = tempFile)
    val file = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq()/*, missingFileParts = Seq()*/)
    file
  }

  "The CsvFileProcessor service " must {

    "validate file data and return errors" in {
      val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
      val result = CsvFileProcessor.validateFile(fileCopied,"Other_Grants_V3.csv",ErsValidator.validateRow)(DataValidator(ConfigFactory.load.getConfig("ers-other-grants-validation-config")))
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
     // result.map(_.errors.size shouldBe  4)
    }

    "throw the correct error message in the validateFile function" in{
      val result = intercept[Exception]{
        def validator(rowData:Seq[String],rowCount:Int,dataValidator:DataValidator): Option[List[ValidationError]] = throw new Exception
        Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)
        val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
        CsvFileProcessor.validateFile(fileCopied, "Other_Grants_V3.csv", validator)(mock[DataValidator])
      }
      result.getMessage shouldEqual  messages("ers.exceptions.dataParser.fileParsingError", "Other_Grants_V3.csv")
    }

    "throw correct exception if an empty csv is given" in {
      val result = intercept[ERSFileProcessingException] {
        val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Acquisition_V3.csv")
        Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Acquisition_V3.csv").toPath)
        val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Acquisition_V3.csv")
        CsvFileProcessor.validateFile(fileCopied,"Other_Acquisition_V3",ErsValidator.validateRow)(DataValidator(ConfigFactory.load.getConfig("ers-other-acquisition-validation-config")))
      }
      result.getMessage shouldEqual messages("ers_check_csv_file.noData", "Other_Acquisition_V3.csv")
    }

  }

  "converter should split by comma" in {
    CsvFileProcessor.converter("a,b,c") shouldBe Array("a","b","c")
  }

}
