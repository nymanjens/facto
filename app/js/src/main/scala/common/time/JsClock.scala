package common.time

import java.time.{Instant, LocalDate, LocalTime}

final class JsClock extends Clock {

  override def now = {
    val date = LocalDate.now()
    val time = LocalTime.now()
    LocalDateTime.of(date, time)
  }

  override def nowInstant = Instant.now()
}
