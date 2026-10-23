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
import utils.FeatureDate

import java.time.{Clock, LocalDate, ZoneId}

class ApplicationConfigSpec extends AnyWordSpecLike with Matchers {
  val servicesConfig: ServicesConfig = mock[ServicesConfig]

  private val zone = ZoneId.of("Europe/London")
  val clock: Clock = mock[Clock]
  when(clock.getZone).thenReturn(zone)

  val appConfig: ApplicationConfig = new ApplicationConfig(servicesConfig) {
    override protected def featureDate: FeatureDate = FeatureDate(clock)
    override lazy val basGatewayHost: String        = "https://example.com"
  }

  "ApplicationConfig" when {
    ".languageMap" should {
      "return correct language map" in {
        val expected = Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))
        appConfig.languageMap mustBe expected
      }
    }

    ".routeToSwitchLanguage" should {
      "return correct route to switch language" in {
        val call: Call = appConfig.routeToSwitchLanguage("cy")
        call.url must include("/language/cy")
      }
    }

    ".getSignOutUrl" should {
      "return a correctly formatted sign-out URL with encoded callback" in {
        val callbackUrl     = "http://localhost:9000/callback"
        val expectedEncoded = java.net.URLEncoder.encode(callbackUrl, "UTF-8")
        val expectedUrl     = s"https://example.com/bas-gateway/sign-out-without-state?continue=$expectedEncoded"
        appConfig.getSignOutUrl(callbackUrl) mustBe expectedUrl
      }
    }

    "useV4andV5SchemeEnabled" should {

      "be enabled only withing the configured date range" in {
        when(servicesConfig.getString("microservice.services.features.dates.use-V4-and-V5-start-enabled-from"))
          .thenReturn("2026-10-15")
        when(servicesConfig.getString("microservice.services.features.dates.use-V4-and-V5-enabled-until"))
          .thenReturn("2027-04-06")

        // before start date
        when(clock.instant()).thenReturn(LocalDate.parse("2026-10-14").atStartOfDay(zone).toInstant)
        appConfig.useV4andV5SchemeEnabledFrom mustBe false

        // between start & end date
        when(clock.instant()).thenReturn(LocalDate.parse("2027-02-01").atStartOfDay(zone).toInstant)
        appConfig.useV4andV5SchemeEnabledFrom mustBe true

        // on the end date
        when(clock.instant()).thenReturn(LocalDate.parse("2027-04-06").atStartOfDay(zone).toInstant)
        appConfig.useV4andV5SchemeEnabledFrom mustBe false

      }
    }

    "useV6andV7SchemeEnabled & displayNonTASSRadioButton" should {

      "be enabled within the configured date range" in {
        when(servicesConfig.getString("microservice.services.features.dates.use-V6-and-V7-enabled-from"))
          .thenReturn("2027-02-01")
        when(servicesConfig.getString("microservice.services.features.dates.non-tass-radio-enabled-from"))
          .thenReturn("2027-02-01")

        // before start date
        when(clock.instant()).thenReturn(LocalDate.parse("2027-01-31").atStartOfDay(zone).toInstant)
        appConfig.useV6andV7SchemeEnabledFrom mustBe false
        appConfig.displayNonTASSRadioButton   mustBe false

        // on the start date
        when(clock.instant()).thenReturn(LocalDate.parse("2027-02-01").atStartOfDay(zone).toInstant)
        appConfig.useV6andV7SchemeEnabledFrom mustBe true
        appConfig.displayNonTASSRadioButton   mustBe true

        // after the start date
        when(clock.instant()).thenReturn(LocalDate.parse("2027-02-02").atStartOfDay(zone).toInstant)
        appConfig.useV6andV7SchemeEnabledFrom mustBe true
        appConfig.displayNonTASSRadioButton   mustBe true
      }
    }
  }

}
