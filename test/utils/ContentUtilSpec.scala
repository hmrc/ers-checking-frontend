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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.ContentUtil.{ErrorMessageKeyPrefixAndScheme, getScheneNameWithShortenedVersion, withArticle}

class ContentUtilSpec extends AnyWordSpecLike with Matchers {

  "ContentUtil" should {

    case class TestCase(inputSchemeType: String, expectedOutputErrorReport: String, expectedOutputSchemeType: String)

    "return the expected error report and scheme type" when
      List(
        TestCase("CSOP", "ers_pdf_error_report.csop", "CSOP"),
        TestCase("EMI", "ers_pdf_error_report.emi", "EMI"),
        TestCase("OTHER", "ers_pdf_error_report.other", "OTHER"),
        TestCase("SAYE", "ers_pdf_error_report.saye", "SAYE"),
        TestCase("SIP", "ers_pdf_error_report.sip", "SIP"),
        TestCase("cSop", "ers_pdf_error_report.csop", "CSOP"),
        TestCase("eMi", "ers_pdf_error_report.emi", "EMI"),
        TestCase("othEr", "ers_pdf_error_report.other", "OTHER"),
        TestCase("saYe", "ers_pdf_error_report.saye", "SAYE"),
        TestCase("sIp", "ers_pdf_error_report.sip", "SIP"),
        TestCase("NON-TASS", "ers_pdf_error_report.non-tass", "Non-TASS"),
        TestCase("Non-TASS", "ers_pdf_error_report.non-tass", "Non-TASS"),
        TestCase("non-tAss", "ers_pdf_error_report.non-tass", "Non-TASS")
      ).foreach((testCase: TestCase) =>
        s"passed a valid scheme type of any case: ${testCase.inputSchemeType}" in {
          getScheneNameWithShortenedVersion(testCase.inputSchemeType) shouldBe ErrorMessageKeyPrefixAndScheme(
            testCase.expectedOutputErrorReport,
            testCase.expectedOutputSchemeType
          )
        }
      )

    "return a tuple containing two empty strings" when
      List(
        TestCase("", "", ""),
        TestCase("not a valid scheme", "", "")
      ).foreach((testCase: TestCase) =>
        s"passed a scheme type or number that isn't linked to a scheme type ${testCase.inputSchemeType}" in {
          getScheneNameWithShortenedVersion(testCase.inputSchemeType) shouldBe ErrorMessageKeyPrefixAndScheme(
            testCase.expectedOutputErrorReport,
            testCase.expectedOutputSchemeType
          )
        }
      )

    "parse withArticle" when {
      "data starts with a consonant" in {
        val string = "bing!"
        withArticle(string) shouldBe "a bing!"
      }
      "data starts with a vowel" in {
        val string = "apple"
        withArticle(string) shouldBe "an apple"
      }
    }
  }

}
