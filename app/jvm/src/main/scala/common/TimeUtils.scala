package common

import scala.collection.immutable.Seq
import java.time.{Instant, LocalDate, Month, ZoneId}
import java.time.format.{DateTimeFormatter, DateTimeParseException}

object TimeUtils {

  def instantAt(year: Int, month: Month, dayOfMonth: Int): Instant = {
    val zone = ZoneId.of("Europe/Paris")
    LocalDate.of(year, month, dayOfMonth).atStartOfDay(zone).toInstant
  }

  def yearAt(instant: Instant): Int = {
    val zone = ZoneId.of("Europe/Paris")
    instant.atZone(zone).toLocalDate.getYear
  }

  def requireStartOfMonth(date: LocalDate): Unit = {
    require(date.getDayOfMonth == 1, s"Date $date should be at the first day of the month.")
  }

  def allMonths: Seq[Month] = Month.values().toList

  /**
    * Parses the incoming date string to a Instant.
    *
    * @param dateString in the form of yyyy-mm-dd, e.g. "2016-03-13".
    * @throws IllegalArgumentException if the given string could not be parsed
    */
  def parseDateString(dateString: String): Instant = {
    try {
      val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
      val zone = ZoneId.of("Europe/Paris")
      localDate.atStartOfDay(zone).toInstant
    } catch {
      case e: DateTimeParseException => throw new IllegalArgumentException(e)
    }
  }
}
