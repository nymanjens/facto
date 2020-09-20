package hydro.common.time

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

import scala.collection.immutable.Seq

object TimeUtils {

  def requireStartOfMonth(date: LocalDate): Unit = {
    require(date.getDayOfMonth == 1, s"Date $date should be at the first day of the month.")
  }

  def allMonths: Seq[Month] = Month.values().toList

  /**
    * Parses the incoming date string to a LocalDateTime.
    *
    * @param dateString in the form of yyyy-mm-dd, e.g. "2016-03-13". Leading zeros may be omitted.
    * @throws IllegalArgumentException if the given string could not be parsed
    */
  def parseDateString(dateString: String): LocalDateTime = {
    require(!dateString.startsWith("-"), dateString)
    require(!dateString.endsWith("-"), dateString)
    val parts = dateString.split("-").toList
    require(dateString.split("-").size == 3, parts)

    val yyyy :: mm :: dd :: Nil = parts
    try {
      LocalDateTime.of(
        LocalDate.of(yyyy.toInt, mm.toInt, dd.toInt),
        LocalTime.MIN,
      )
    } catch {
      case e: NumberFormatException => throw new IllegalArgumentException(e)
      case e: DateTimeException     => throw new IllegalArgumentException(e)
    }
  }
}
