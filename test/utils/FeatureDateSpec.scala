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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{Clock, LocalDate, ZoneId}

class FeatureDateSpec extends AnyWordSpecLike {

  private val zone                         = ZoneId.of("Europe/London")

  private def clockAt(date: String): Clock =
    Clock.fixed(
      LocalDate.parse(date).atStartOfDay(zone).toInstant,
      zone
    )

  "isEnabledFrom"  should {
    "be false before the start date" in {
      FeatureDate.isEnabledFrom(
        LocalDate.parse("2026-11-02"),
        clockAt("2026-11-01")
      ) mustBe false
    }
    "be true on the start date" in {
      FeatureDate.isEnabledFrom(
        LocalDate.parse("2026-11-02"),
        clockAt("2026-11-02")
      ) mustBe true
    }
  }

  "isEnabledUntil" should {
    "be true the day before the end date" in {
      FeatureDate.isEnabledUntil(
        LocalDate.parse("2027-04-06"),
        clockAt("2027-04-05")
      ) mustBe true
    }
    "be false on the end date" in {
      FeatureDate.isEnabledUntil(
        LocalDate.parse("2027-04-06"),
        clockAt("2027-04-06")
      ) mustBe false
    }
  }

}
