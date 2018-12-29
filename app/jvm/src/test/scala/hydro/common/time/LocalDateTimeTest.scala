package hydro.common.time

import java.time.Duration
import java.time.Month._

import app.common.testing.TestUtils._
import org.specs2.mutable._

class LocalDateTimeTest extends Specification {

  "plus #1" in {
    val date = localDateTimeOfEpochSecond(1030507)
    val duration = Duration.ofSeconds(204060)
    (date plus duration) mustEqual localDateTimeOfEpochSecond(1234567)
  }

  "plus #2" in {
    val date = LocalDateTime.of(2012, MAY, 12, 12, 30)
    val duration = Duration.ofDays(2) plus Duration.ofHours(12)
    (date plus duration) mustEqual LocalDateTime.of(2012, MAY, 15, 0, 30)
  }

  "minus #1" in {
    val date = localDateTimeOfEpochSecond(1030507)
    val duration = Duration.ofSeconds(204060)
    (date minus duration) mustEqual localDateTimeOfEpochSecond(826447)
  }

  "minus #2" in {
    val date = LocalDateTime.of(2012, MAY, 12, 10, 30)
    val duration = Duration.ofDays(2) plus Duration.ofHours(12)
    (date minus duration) mustEqual LocalDateTime.of(2012, MAY, 9, 22, 30)
  }
}
