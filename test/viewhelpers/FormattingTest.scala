package viewhelpers

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._

import org.joda.time.DateTime

import common.{TimeUtils, Clock, DatedMonth, MonthRange}
import common.TimeUtils.{dateAt, January, February, March, April, May, December}
import common.testing.HookedSpecification

@RunWith(classOf[JUnitRunner])
class FormattingTest extends HookedSpecification {

  override def before = Clock.setTimeForTest(dateAt(2010, April, 4))
  override def afterAll = Clock.cleanupAfterTest

  "formatDate()" in {
    Formatting.formatDate(dateAt(2010, April, 4)) mustEqual "Today"
    Formatting.formatDate(dateAt(2010, April, 3)) mustEqual "Yesterday"
    Formatting.formatDate(dateAt(2010, April, 5)) mustEqual "Tomorrow"
    Formatting.formatDate(dateAt(2010, March, 31)) mustEqual "Wed, 31 Mar"
    Formatting.formatDate(dateAt(2010, April, 6)) mustEqual "Tue, 6 Apr"
    Formatting.formatDate(dateAt(2010, January, 1)) mustEqual "1 Jan"
    Formatting.formatDate(dateAt(2009, December, 31)) mustEqual "31 Dec '09"
    Formatting.formatDate(dateAt(2012, December, 31)) mustEqual "31 Dec '12"
  }
}
