package common.time

import common.time.LocalDateTime
import java.time.{LocalDate, LocalTime, ZoneId}

final class JvmClock extends Clock {

  override def now: LocalDateTime = {
    // TODO: Inject zone
    val zone = ZoneId.of("Europe/Paris")
    val date = LocalDate.now(zone)
    val time = LocalTime.now(zone)
    LocalDateTime.of(date, time)
  }
}
