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

package utils

import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

class UploadedFileUtilSpec  extends UnitSpec with MockitoSugar {

  "UploadedFileUtil ODS" should {

    "return positively for an ods file" in {
      UploadedFileUtil.checkODSFileType("abc.ods") shouldBe true
    }

    "return positively for an ods file with extension in upper case" in {
      UploadedFileUtil.checkODSFileType("abc.ODS") shouldBe true
    }

    "return negatively for other files" in {
      UploadedFileUtil.checkODSFileType("abc.doc") shouldBe false
    }

  }

  "UploadedFileUtil CSV" should {

    "return positively for an csv file" in {
      UploadedFileUtil.checkCSVFileType("abc.csv") shouldBe true
    }

    "return negatively for other files" in {
      UploadedFileUtil.checkCSVFileType("abc.doc") shouldBe false
    }

  }
}
