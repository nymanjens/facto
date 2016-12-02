package common.time

import common.time.LocalDateTime
import java.time.{LocalDate, LocalTime}

final class JsClock extends Clock {

  override def now: LocalDateTime = {
    val date = LocalDate.now()
    val time = LocalTime.now()
    LocalDateTime.of(date, time)
  }
}
