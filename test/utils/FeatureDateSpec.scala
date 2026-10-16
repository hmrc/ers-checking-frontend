package utils

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.{Clock, LocalDate, ZoneId}

class FeatureDateSpec extends AnyWordSpecLike {

  private val zone = ZoneId.of("Europe/London")
  private def clockAt(date: String): Clock =
    Clock.fixed(
      LocalDate.parse(date).atStartOfDay(zone).toInstant,
      zone
    )

  "isEnabledFrom" should {
    "be false before the start date" in {
      FeatureDate.isEnabledFrom(
        LocalDate.parse("2026-11-02"), clockAt("2026-11-01")
      ) mustBe false
    }
    "be true on the start date" in {
      FeatureDate.isEnabledFrom(
        LocalDate.parse("2026-11-02"), clockAt("2026-11-02")
      ) mustBe true
    }
  }
  "isEnabledUntil" should {
    "be true the day before the end date" in {
      FeatureDate.isEnabledUntil(
        LocalDate.parse("2027-04-06"), clockAt("2027-04-05")
      ) mustBe true
    }
    "be false on the end date" in {
      FeatureDate.isEnabledUntil(
        LocalDate.parse("2027-04-06"), clockAt("2027-04-06")
      ) mustBe false
    }
  }
}
