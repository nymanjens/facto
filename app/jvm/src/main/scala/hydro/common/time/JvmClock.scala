package hydro.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

final class JvmClock(zone: ZoneId) extends Clock {

  private val initialInstant: Instant = Instant.now
  private val initialNanos: Long = System.nanoTime

  override def now = {
    val date = LocalDate.now(zone)
    val time = LocalTime.now(zone)
    LocalDateTime.of(date, time)
  }

  override def nowInstant = initialInstant plusNanos (System.nanoTime - initialNanos)
}
