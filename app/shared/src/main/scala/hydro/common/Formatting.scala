package hydro.common

import java.lang.Math.abs
import java.time.DayOfWeek._
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Month._

import hydro.common.time.Clock
import hydro.common.time.LocalDateTime

object Formatting {
  // Note: Cannot use DateTimeFormatter as it isn't supported by scala.js

  private val monthToMessageKey: Map[Month, String] = Map(
    JANUARY -> "app.date.month.jan.abbrev",
    FEBRUARY -> "app.date.month.feb.abbrev",
    MARCH -> "app.date.month.mar.abbrev",
    APRIL -> "app.date.month.apr.abbrev",
    MAY -> "app.date.month.may.abbrev",
    JUNE -> "app.date.month.jun.abbrev",
    JULY -> "app.date.month.jul.abbrev",
    AUGUST -> "app.date.month.aug.abbrev",
    SEPTEMBER -> "app.date.month.sep.abbrev",
    OCTOBER -> "app.date.month.oct.abbrev",
    NOVEMBER -> "app.date.month.nov.abbrev",
    DECEMBER -> "app.date.month.dec.abbrev"
  )

  private val dayOfWeekToMessageKey: Map[DayOfWeek, String] = Map(
    MONDAY -> "app.date.dayofweek.mon.abbrev",
    TUESDAY -> "app.date.dayofweek.tue.abbrev",
    WEDNESDAY -> "app.date.dayofweek.wed.abbrev",
    THURSDAY -> "app.date.dayofweek.thu.abbrev",
    FRIDAY -> "app.date.dayofweek.fri.abbrev",
    SATURDAY -> "app.date.dayofweek.sat.abbrev",
    SUNDAY -> "app.date.dayofweek.sun.abbrev"
  )

  def formatDate(dateTime: LocalDateTime)(implicit i18n: I18n, clock: Clock): String = {
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
        i18n("app.today")
      } else if (date.getDayOfYear == now.getDayOfYear - 1) {
        i18n("app.yesterday")
      } else if (date.getDayOfYear == now.getDayOfYear + 1) {
        i18n("app.tomorrow")
      } else if (dayDifference < 7) {
        s"$dayOfWeek, $dayMonthString"
      } else {
        dayMonthString
      }
    } else {
      s"$dayMonthString '$yearString"
    }
  }

  def formatDateTime(dateTime: LocalDateTime)(implicit i18n: I18n): String = {
    val date = dateTime.toLocalDate
    val monthString = formatMonth(date)
    val timeString = dateTime.toLocalTime.toString take 5 // hack to get time in format "HH:mm"
    s"${date.getDayOfMonth} $monthString ${date.getYear}, $timeString"
  }

  private def formatMonth(date: LocalDate)(implicit i18n: I18n): String = {
    i18n(monthToMessageKey(date.getMonth))
  }

  private def formatDayOfWeek(date: LocalDate)(implicit i18n: I18n): String = {
    i18n(dayOfWeekToMessageKey(date.getDayOfWeek))
  }
}
