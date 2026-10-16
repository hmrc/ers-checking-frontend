package utils

import java.time.{Clock, LocalDate}

object FeatureDate {

   def isEnabledFrom(date: LocalDate,clock : Clock): Boolean =
    !LocalDate.now(clock).isBefore(date)

   def isEnabledUntil(date: LocalDate, clock : Clock): Boolean =
    LocalDate.now(clock).isBefore(date)
}
