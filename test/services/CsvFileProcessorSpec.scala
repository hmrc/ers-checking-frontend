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
import uk.gov.hmrc.services.validation.{DataValidator, ValidationError}
import controllers.Fixtures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import services.ERSTemplatesInfo._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class CsvFileProcessorSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
  if(fileCopied.exists)
    {
      fileCopied.getCanonicalFile.delete()
      fileCopied.getAbsoluteFile.delete()
    }

  val data =  Iterator(Array("25 Jan 2015","1234567","10.1","10.123"))
  val file = new File(System.getProperty("user.dir") + "/test/resources/Other_Grants_V3.csv")
  Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)

  "The CsvFileProcessor service " must {

    "validate file data and return errors" in {
      val validator = ERSValidationConfigs.getValidator(ersSheets("Other_Grants_V3").configFileName)
      val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
      val result = CsvFileProcessor.validateFile(fileCopied,validator, "Other_Grants_V3.csv")
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
        val validator = DataValidator
        Files.copy(file.toPath,new java.io.File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv").toPath)
        val fileCopied = new File(System.getProperty("user.dir") + "/test/resources/copy/Other_Grants_V3.csv")
        CsvFileProcessor.validateFile(fileCopied, validator, "Other_Grants_V3.csv")
      }
      result.getMessage shouldEqual  Messages("ers.exceptions.dataParser.fileParsingError", "Other_Grants_V3.csv")
    }

  }

  "converter should split by comma" in {
    CsvFileProcessor.converter("a,b,c") shouldBe Array("a","b","c")
  }

  def getMockFileCSV = {
    val userDirectory = System.getProperty("user.dir")
    val tempFile = TemporaryFile(new java.io.File(userDirectory+"/test/resources/copy/Other_Grants_V3").toString, ".csv")
    //val tempFile = TemporaryFile(new java.io.File("/test/the.csv"))
    val part = FilePart[TemporaryFile](key = "fileParam", filename = "Other_Grants_V3.csv", contentType = Some("Content-Type: multipart/form-data"), ref = tempFile)
    val file = MultipartFormData(dataParts = Map(), files = Seq(part), badParts = Seq(), missingFileParts = Seq())
    file
  }
}
