package common

import java.lang.Math.abs
import java.time.Month._
import java.time.DayOfWeek._
import common.time.LocalDateTime
import java.time.{LocalDate, Month, DayOfWeek}
import common.time.Clock

object Formatting {
  // Note: Cannot use DateTimeFormatter as it isn't supported by scala.js

  private val monthToMessageKey: Map[Month, String] = Map(
    JANUARY -> "facto.date.month.jan.abbrev",
    FEBRUARY -> "facto.date.month.feb.abbrev",
    MARCH -> "facto.date.month.mar.abbrev",
    APRIL -> "facto.date.month.apr.abbrev",
    MAY -> "facto.date.month.may.abbrev",
    JUNE -> "facto.date.month.jun.abbrev",
    JULY -> "facto.date.month.jul.abbrev",
    AUGUST -> "facto.date.month.aug.abbrev",
    SEPTEMBER -> "facto.date.month.sep.abbrev",
    OCTOBER -> "facto.date.month.oct.abbrev",
    NOVEMBER -> "facto.date.month.nov.abbrev",
    DECEMBER -> "facto.date.month.dec.abbrev"
  )

  private val dayOfWeekToMessageKey: Map[DayOfWeek, String] = Map(
    MONDAY -> "facto.date.dayofweek.mon",
    TUESDAY -> "facto.date.dayofweek.tue",
    WEDNESDAY -> "facto.date.dayofweek.wed",
    THURSDAY -> "facto.date.dayofweek.thu",
    FRIDAY -> "facto.date.dayofweek.fri",
    SATURDAY -> "facto.date.dayofweek.sat",
    SUNDAY -> "facto.date.dayofweek.sun"
  )

  def formatDate(dateTime: LocalDateTime)
                (implicit i18n: I18n, clock: Clock): String = {
    val now = clock.now.toLocalDate
    val date = dateTime.toLocalDate

    val yearString = date.getYear.toString takeRight 2
    val dayMonthString = {
      val monthString = formatMonth(date)
      s"${date.getDayOfMonth} $monthString"
    }
    val dayOfWeek = formatDayOfWeek(date)

    if (date.getYear == now.getYear) {
      val dayDifference = abs(now.getDayOfYear - date.getDayOfYear)

      if (date.getDayOfYear == now.getDayOfYear) {
        i18n("facto.today")
      } else if (date.getDayOfYear == now.getDayOfYear - 1) {
        i18n("facto.yesterday")
      } else if (date.getDayOfYear == now.getDayOfYear + 1) {
        i18n("facto.tomorrow")
      } else if (dayDifference < 7) {
        s"$dayOfWeek, $dayMonthString"
      } else {
        dayMonthString
      }
    } else {
      s"$dayMonthString '$yearString"
    }
  }

  def formatDateTime(dateTime: LocalDateTime)
                    (implicit i18n: I18n): String = {
    val date = dateTime.toLocalDate
    val monthString = formatMonth(date)
    val timeString = dateTime.toLocalTime.toString take 5 // hack to get time in format "HH:mm"
    s"${date.getDayOfMonth} $monthString ${date.getYear}, $timeString"
  }

  private def formatMonth(date: LocalDate)
                         (implicit i18n: I18n): String = {
    i18n(monthToMessageKey(date.getMonth))
  }

  private def formatDayOfWeek(date: LocalDate)
                             (implicit i18n: I18n): String = {
    i18n(dayOfWeekToMessageKey(date.getDayOfWeek))
  }
}
