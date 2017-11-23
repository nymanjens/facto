package common.testing

import java.time.Month.JANUARY

import common.time.{Clock, LocalDateTime}

final class FakeClock extends Clock {

  @volatile private var currentTime: LocalDateTime = FakeClock.defaultTime

  override def now = currentTime

  def setTime(time: LocalDateTime) = {
    currentTime = time
  }
}

object FakeClock {
  val defaultTime: LocalDateTime = LocalDateTime.of(2000, JANUARY, 1, 0, 0)
}
