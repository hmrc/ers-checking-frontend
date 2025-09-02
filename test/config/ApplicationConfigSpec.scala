/*
 * Copyright 2025 HM Revenue & Customs
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

package config

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class ApplicationConfigSpec extends AnyWordSpecLike with Matchers {

  "ApplicationConfig" when {
    ".languageMap" should {
      "return correct language map" in {
        val servicesConfig = mock[ServicesConfig]
        val appConfig = new ApplicationConfig(servicesConfig)
        val expected = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))
        appConfig.languageMap mustBe expected
      }
    }

    ".routeToSwitchLanguage" should {
      "return correct route to switch language" in {
        val servicesConfig = mock[ServicesConfig]
        val appConfig = new ApplicationConfig(servicesConfig)
        val call: Call = appConfig.routeToSwitchLanguage("cy")
        call.url must include("/language/cy")
      }
    }

    ".getSignOutUrl" should {
      "return a correctly formatted sign-out URL with encoded callback" in {
        val mockServicesConfig = mock[ServicesConfig]
        val appConfig = new ApplicationConfig(mockServicesConfig) {
          override lazy val basGatewayHost: String = "https://example.com"
        }
        val callbackUrl = "http://localhost:9000/callback"
        val expectedEncoded = java.net.URLEncoder.encode(callbackUrl, "UTF-8")
        val expectedUrl = s"https://example.com/bas-gateway/sign-out-without-state?continue=$expectedEncoded"
        appConfig.getSignOutUrl(callbackUrl) mustBe expectedUrl
      }
    }
  }
}


