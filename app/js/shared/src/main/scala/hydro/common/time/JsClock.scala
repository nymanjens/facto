package hydro.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

final class JsClock extends Clock {

  override def now = {
    val date = LocalDate.now()
    val time = LocalTime.now()
    LocalDateTime.of(date, time)
  }

  override def nowInstant = Instant.now()
}
