package common.time

import com.google.inject._
import common.time.LocalDateTime
import java.time.{LocalDate, LocalTime, ZoneId}
import models.accounting.config.Config

final class JvmClock @Inject()(accountingConfig: Config) extends Clock {
  val zone = ZoneId.of(accountingConfig.constants.zoneId)

  override def now: LocalDateTime = {
    val date = LocalDate.now(zone)
    val time = LocalTime.now(zone)
    LocalDateTime.of(date, time)
  }
}
