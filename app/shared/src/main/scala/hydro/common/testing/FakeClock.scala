package hydro.common.testing

import java.time.Instant
import java.time.Month.JANUARY

import hydro.common.time.Clock
import hydro.common.time.LocalDateTime

final class FakeClock extends Clock {

  @volatile private var currentLocalDateTime: LocalDateTime = FakeClock.defaultLocalDateTime
  @volatile private var currentInstant: Instant = FakeClock.defaultInstant

  override def now = currentLocalDateTime
  override def nowInstant = currentInstant

  def setNow(localDateTime: LocalDateTime): Unit = {
    currentLocalDateTime = localDateTime
  }

  def setNowInstant(instant: Instant): Unit = {
    currentInstant = instant
  }
}

object FakeClock {
  val defaultLocalDateTime: LocalDateTime = LocalDateTime.of(2000, JANUARY, 1, 0, 0)
  val defaultInstant: Instant = Instant.ofEpochMilli(9812093809912L)
}
