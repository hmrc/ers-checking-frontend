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

package config

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate

class ApplicationConfigSpec extends AnyWordSpecLike with Matchers {
  val servicesConfig: ServicesConfig = mock[ServicesConfig]

  "ApplicationConfig" when {
    ".languageMap" should {
      "return correct language map" in {
        val appConfig = new ApplicationConfig(servicesConfig)
        val expected  = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))
        appConfig.languageMap mustBe expected
      }
    }

    ".routeToSwitchLanguage" should {
      "return correct route to switch language" in {
        val appConfig  = new ApplicationConfig(servicesConfig)
        val call: Call = appConfig.routeToSwitchLanguage("cy")
        call.url must include("/language/cy")
      }
    }

    ".getSignOutUrl" should {
      "return a correctly formatted sign-out URL with encoded callback" in {
        val appConfig       = new ApplicationConfig(servicesConfig) {
          override lazy val basGatewayHost: String = "https://example.com"
        }
        val callbackUrl     = "http://localhost:9000/callback"
        val expectedEncoded = java.net.URLEncoder.encode(callbackUrl, "UTF-8")
        val expectedUrl     = s"https://example.com/bas-gateway/sign-out-without-state?continue=$expectedEncoded"
        appConfig.getSignOutUrl(callbackUrl) mustBe expectedUrl
      }
    }

    "startPageBannerEnabled" should {

      "return true when the start date has been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.startPageBannerEnabledFrom mustBe true
      }

      "return false when the start date has not been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-enabled-from"))
          .thenReturn(LocalDate.now().plusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.startPageBannerEnabledFrom mustBe false
      }
    }

    "startPageBannerPara2Enabled" should {

      "return true when the banner has started and para 2 has not been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-para2-enabled-until"))
          .thenReturn(LocalDate.now().plusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.startPageBannerParaTwoEnabledFrom mustBe true
      }

      "return false when the banner start date has not been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-enabled-from"))
          .thenReturn(LocalDate.now().plusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.startPageBannerParaTwoEnabledFrom mustBe false
      }

      "return false when para 2 end date has been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        when(servicesConfig.getString("microservice.services.features.dates.start-page-banner-para2-enabled-until"))
          .thenReturn(LocalDate.now().toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.startPageBannerParaTwoEnabledFrom mustBe false
      }
    }

    "useV4andV5SchemeEnabled" should {

      "return true within the configured date range" in {
        when(servicesConfig.getString("microservice.services.features.dates.use-V4-and-V5-start-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        when(servicesConfig.getString("microservice.services.features.dates.use-V4-and-V5-enabled-until"))
          .thenReturn(LocalDate.now().plusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.useV4andV5SchemeEnabledFrom mustBe true
      }
    }

    "useV6andV7SchemeEnabled & displayNonTASSRadioButton" should {

      "return true when the configured date has been reached" in {
        when(servicesConfig.getString("microservice.services.features.dates.use-V6-and-V7-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        when(servicesConfig.getString("microservice.services.features.dates.non-tass-radio-enabled-from"))
          .thenReturn(LocalDate.now().minusDays(1).toString)
        val applicationConfig = new ApplicationConfig(servicesConfig)
        applicationConfig.useV6andV7SchemeEnabledFrom mustBe true
        applicationConfig.displayNonTASSRadioButton   mustBe true

      }
    }
  }

}
