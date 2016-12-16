package common.time

import common.time.LocalDateTimes.createDateTime
import org.specs2.mutable._

import java.util.{Date, Calendar}

import TimeUtils.dateAt

class TimeUtilsTest extends Specification {

  "dateAt" in {
    val date = createDateTime(1998, TimeUtils.March, 3)
    date.getYear mustEqual 1998
    date.getMonthOfYear mustEqual 3
    date.getDayOfMonth mustEqual 3
    date.getHourOfDay mustEqual 0
    date.getSecondOfDay mustEqual 0
    date.getMillisOfDay mustEqual 0

    // Compare to java.util.Date/Calendar.
    val calendar = new Calendar.Builder().setInstant(new Date(date.getMillis)).build()

    // Check all expected derived properties.
    calendar.get(Calendar.YEAR) mustEqual 1998
    calendar.get(Calendar.MONTH) mustEqual Calendar.MARCH
    calendar.get(Calendar.DAY_OF_MONTH) mustEqual 3
    calendar.get(Calendar.HOUR) mustEqual 0
    calendar.get(Calendar.SECOND) mustEqual 0
    calendar.get(Calendar.MILLISECOND) mustEqual 0

    // Check that subtracting only a second leads to the previous day.
    calendar.add(Calendar.SECOND, -1)
    calendar.get(Calendar.DAY_OF_MONTH) mustEqual 2
  }

  "parseDateString" in {
    // TODO
  }
}
