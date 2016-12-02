package common.time

import scala.collection.immutable.Seq
import common.time.LocalDateTime
import java.time.{LocalDate, Month, ZoneId}
import java.time.format.{DateTimeFormatter, DateTimeParseException}

object TimeUtils {

  def requireStartOfMonth(date: LocalDate): Unit = {
    require(date.getDayOfMonth == 1, s"Date $date should be at the first day of the month.")
  }

  def allMonths: Seq[Month] = Month.values().toList

  /**
    * Parses the incoming date string to a LocalDateTime.
    *
    * @param dateString in the form of yyyy-mm-dd, e.g. "2016-03-13".
    * @throws IllegalArgumentException if the given string could not be parsed
    */
  def parseDateString(dateString: String): LocalDateTime = {
    try {
      val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
      LocalDateTime.ofJavaLocalDateTime(localDate.atStartOfDay())
    } catch {
      case e: DateTimeParseException => throw new IllegalArgumentException(e)
    }
  }
}
