package common.time

import java.time.{Duration, LocalDate}
import java.time.Month._

import common.testing.TestUtils._
import org.specs2.mutable._

class LocalDateTimeTest extends Specification {

  "plus #1" in {
    val date = localDateTimeOfEpochMilli(1030507)
    val duration = Duration.ofMillis(204060)
    (date plus duration) mustEqual localDateTimeOfEpochMilli(1234567)
  }

  "plus #2" in {
    val date = LocalDateTime.of(2012, MAY, 12, 12, 30)
    val duration = Duration.ofDays(2) plus Duration.ofHours(12)
    (date plus duration) mustEqual LocalDateTime.of(2012, MAY, 15, 0, 30)
  }
}
