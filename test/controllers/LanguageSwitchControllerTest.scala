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

package controllers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{ControllerComponents, Cookie, Cookies}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.language.LanguageUtils
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike


class LanguageSwitchControllerTest extends AnyWordSpecLike with Matchers with OptionValues with GuiceOneAppPerSuite {
  val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  val langUtils : LanguageUtils = app.injector.instanceOf[LanguageUtils]
  val testLanguageSwitchController = new LanguageSwitchController(langUtils : LanguageUtils, controllerComponents : ControllerComponents)

  "Hitting language selection endpoint" must {
    "redirect to Welsh translated start page if Welsh language is selected" in {
      val request = FakeRequest()
      val result = testLanguageSwitchController.switchToLanguage("cymraeg")(request)
      val resultCookies: Cookies = cookies(result)
      resultCookies.size shouldBe 1
      val cookie: Cookie = resultCookies.head
      cookie.name shouldBe "PLAY_LANG"
      cookie.value shouldBe "cy"
    }

    "redirect to English translated start page if English language is selected" in {
      val request = FakeRequest()
      val result = testLanguageSwitchController.switchToLanguage("english")(request)
      val resultCookies: Cookies = cookies(result)
      resultCookies.size shouldBe 1
      val cookie: Cookie = resultCookies.head
      cookie.name shouldBe "PLAY_LANG"
      cookie.value shouldBe "en"
    }
  }
}