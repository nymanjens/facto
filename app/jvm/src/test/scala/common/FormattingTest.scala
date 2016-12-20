package common

import java.time.Month._

import com.google.inject._
import common.testing._
import common.time.LocalDateTimes.createDateTime
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class FormattingTest extends HookedSpecification {

  @Inject implicit private val fakeClock: FakeClock = null
  @Inject implicit private val fakeI18n: FakeI18n = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
    setFakeI18nMappings()
    fakeClock.setTime(createDateTime(2010, APRIL, 4))
  }

  "formatDate()" in {
    Formatting.formatDate(createDateTime(2010, MARCH, 31)) mustEqual "Wed, 31 Mar"
    Formatting.formatDate(createDateTime(2010, APRIL, 1)) mustEqual "Thu, 1 Apr"
    Formatting.formatDate(createDateTime(2010, APRIL, 2)) mustEqual "Fri, 2 Apr"
    Formatting.formatDate(createDateTime(2010, APRIL, 3)) mustEqual "Yesterday"
    Formatting.formatDate(createDateTime(2010, APRIL, 4)) mustEqual "Today"
    Formatting.formatDate(createDateTime(2010, APRIL, 5)) mustEqual "Tomorrow"
    Formatting.formatDate(createDateTime(2010, APRIL, 6)) mustEqual "Tue, 6 Apr"
    Formatting.formatDate(createDateTime(2010, APRIL, 7)) mustEqual "Wed, 7 Apr"

    Formatting.formatDate(createDateTime(2010, JANUARY, 1)) mustEqual "1 Jan"
    Formatting.formatDate(createDateTime(2009, DECEMBER, 31)) mustEqual "31 Dec '09"

    Formatting.formatDate(createDateTime(2012, JANUARY, 12)) mustEqual "12 Jan '12"
    Formatting.formatDate(createDateTime(2012, FEBRUARY, 12)) mustEqual "12 Feb '12"
    Formatting.formatDate(createDateTime(2012, MARCH, 12)) mustEqual "12 Mar '12"
    Formatting.formatDate(createDateTime(2012, APRIL, 12)) mustEqual "12 Apr '12"
    Formatting.formatDate(createDateTime(2012, MAY, 12)) mustEqual "12 May '12"
    Formatting.formatDate(createDateTime(2012, JUNE, 12)) mustEqual "12 June '12"
    Formatting.formatDate(createDateTime(2012, JULY, 12)) mustEqual "12 July '12"
    Formatting.formatDate(createDateTime(2012, AUGUST, 12)) mustEqual "12 Aug '12"
    Formatting.formatDate(createDateTime(2012, SEPTEMBER, 12)) mustEqual "12 Sept '12"
    Formatting.formatDate(createDateTime(2012, OCTOBER, 12)) mustEqual "12 Oct '12"
    Formatting.formatDate(createDateTime(2012, NOVEMBER, 12)) mustEqual "12 Nov '12"
    Formatting.formatDate(createDateTime(2012, DECEMBER, 12)) mustEqual "12 Dec '12"
  }

  private def setFakeI18nMappings(): Unit = {
    fakeI18n.setMappings(
      "facto.today" -> "Today",
      "facto.yesterday" -> "Yesterday",
      "facto.tomorrow" -> "Tomorrow",
      "facto.date.month.jan.abbrev" -> "Jan",
      "facto.date.month.feb.abbrev" -> "Feb",
      "facto.date.month.mar.abbrev" -> "Mar",
      "facto.date.month.apr.abbrev" -> "Apr",
      "facto.date.month.may.abbrev" -> "May",
      "facto.date.month.jun.abbrev" -> "June",
      "facto.date.month.jul.abbrev" -> "July",
      "facto.date.month.aug.abbrev" -> "Aug",
      "facto.date.month.sep.abbrev" -> "Sept",
      "facto.date.month.oct.abbrev" -> "Oct",
      "facto.date.month.nov.abbrev" -> "Nov",
      "facto.date.month.dec.abbrev" -> "Dec",
      "facto.date.dayofweek.mon.abbrev" -> "Mon",
      "facto.date.dayofweek.tue.abbrev" -> "Tue",
      "facto.date.dayofweek.wed.abbrev" -> "Wed",
      "facto.date.dayofweek.thu.abbrev" -> "Thu",
      "facto.date.dayofweek.fri.abbrev" -> "Fri",
      "facto.date.dayofweek.sat.abbrev" -> "Sat",
      "facto.date.dayofweek.sun.abbrev" -> "Sun")
  }
}
