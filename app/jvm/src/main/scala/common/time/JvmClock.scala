package common.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

import com.google.inject._
import models.accounting.config.Config

final class JvmClock @Inject()(accountingConfig: Config) extends Clock {
  val zone = ZoneId.of(accountingConfig.constants.zoneId)

  private val initialInstant: Instant = Instant.now
  private val initialNanos: Long = System.nanoTime

  override def now = {
    val date = LocalDate.now(zone)
    val time = LocalTime.now(zone)
    LocalDateTime.of(date, time)
  }

  override def nowInstant = initialInstant plusNanos (System.nanoTime - initialNanos)
}
