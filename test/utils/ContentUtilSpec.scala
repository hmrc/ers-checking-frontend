/*
 * Copyright 2021 HM Revenue & Customs
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

class ContentUtilSpec extends UnitSpec with MockitoSugar {

  class TestUtil extends ContentUtil

  "ContentUtil" should {
    val contentUtil = new TestUtil
    val data = List(
      ("csop", "Company Share Option Plan"),
      ("emi", "Enterprise Management Incentives"),
      ("other", "Other"),
      ("saye", "Save As You Earn"),
      ("sip", "Share Incentive Plan"),
      ("", "an invalid thing")
    )
    for(schemeType <- data) {
      s"return scheme name and abbreviation for ${schemeType._2}" in {
        contentUtil.getSchemeName(schemeType._1)._2 shouldBe schemeType._1.toUpperCase
      }
    }

    "parse withArticle" when {
      "data starts with a consonant" in {
        val string = "bing!"
        contentUtil.withArticle(string) shouldBe "a bing!"
      }
      "data starts with a vowel" in {
        val string = "apple"
        contentUtil.withArticle(string) shouldBe "an apple"
      }
    }
  }
}
