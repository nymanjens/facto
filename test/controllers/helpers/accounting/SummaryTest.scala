package controllers.helpers.accounting

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._

import play.twirl.api.Html
import org.joda.time.DateTime

import common.{TimeUtils, Clock, DatedMonth, MonthRange}
import common.TimeUtils.{dateAt, January, February, March, April, May}
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing.HookedSpecification
import models.accounting._
import models.accounting.config.{Config, Account, Category}

@RunWith(classOf[JUnitRunner])
class SummaryTest extends HookedSpecification {

  override def before = Clock.setTimeForTest(dateAt(2010, April, 4))
  override def afterAll = Clock.cleanupAfterTest

  "Summary.fetchSummary()" should {
    "combine transactions in same month and category" in new WithApplication {
      val trans1 = persistTransaction(groupId = 1, flow = Money(200), date = dateAt(2009, February, 2))
      val trans2 = persistTransaction(groupId = 1, flow = Money(201), date = dateAt(2009, February, 20))
      val trans3 = persistTransaction(groupId = 1, flow = Money(202), date = dateAt(2009, March, 1))

      val summary = Summary.fetchSummary(testAccount, 2009)

      summary.yearToSummary.keySet mustEqual Set(2009, 2010)
      summary.yearToSummary(2009).cell(testCategory, february(2009)).entries must contain(exactly(GeneralEntry(Seq(trans1, trans2))))
      for (cell <- summary.yearToSummary(2010).cells.values.asScala) {
        cell.totalFlow mustEqual Money(0)
      }
    }

    "caculate monthRangeForAverages" in new WithApplication {
      persistTransaction(groupId = 1, flow = Money(200), date = dateAt(2009, February, 2))
      persistTransaction(groupId = 1, flow = Money(201), date = dateAt(2009, February, 20))
      persistTransaction(groupId = 1, flow = Money(202), date = dateAt(2009, March, 1))
      persistTransaction(groupId = 1, flow = Money(202), date = dateAt(2010, May, 4))

      val summary = Summary.fetchSummary(testAccount, 2009)

      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2009, February, 1), dateAt(2010, April, 1))
    }

    "return successfully when there are no transactions" in new WithApplication {
      val summary = Summary.fetchSummary(testAccount, 2009)
      summary.totalRowTitles must not(beEmpty)
    }

    "ignore the current and future months when calculating the averages" in new WithApplication {
      persistTransaction(groupId = 1, flow = Money(999), date = dateAt(2009, April, 2))
      persistTransaction(groupId = 1, flow = Money(100), date = dateAt(2010, February, 2))
      persistTransaction(groupId = 1, flow = Money(112), date = dateAt(2010, March, 2))
      persistTransaction(groupId = 1, flow = Money(120), date = dateAt(2010, April, 2))
      persistTransaction(groupId = 1, flow = Money(130), date = dateAt(2010, May, 2))
      persistTransaction(groupId = 1, flow = Money(1999), date = dateAt(2011, April, 2))

      val summary = Summary.fetchSummary(testAccount, 2009)

      summary.yearToSummary(2010).categoryToAverages(testCategory) mustEqual Money(71)
      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2009, April, 1), dateAt(2010, April, 1))
    }


    "ignore the pre-facto months when calculating the averages" in new WithApplication {
      persistTransaction(groupId = 1, flow = Money(100), date = dateAt(2010, February, 2))
      persistTransaction(groupId = 1, flow = Money(112), date = dateAt(2010, March, 2))
      persistTransaction(groupId = 1, flow = Money(120), date = dateAt(2010, April, 2))

      val summary = Summary.fetchSummary(testAccount, 2009)

      summary.yearToSummary(2010).categoryToAverages(testCategory) mustEqual Money(106)
      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2010, February, 1), dateAt(2010, April, 1))
    }

    "calculates totals" in new WithApplication {
      persistTransaction(groupId = 1, flow = Money(3), date = dateAt(2010, January, 2), category = testCategoryA)
      persistTransaction(groupId = 1, flow = Money(100), date = dateAt(2010, February, 2), category = testCategoryA)
      persistTransaction(groupId = 1, flow = Money(102), date = dateAt(2010, February, 2), category = testCategoryB)

      val summary = Summary.fetchSummary(testAccount, 2009)

      val totalRows = summary.yearToSummary(2010).totalRows
      totalRows must haveSize(2)
      totalRows(0).rowTitleHtml mustEqual Html("<b>Total</b>")
      totalRows(0).monthToTotal(february(2010)) mustEqual Money(202)
      totalRows(0).yearlyAverage mustEqual Money(68)
      totalRows(1).rowTitleHtml mustEqual Html("<b>Total</b> (without catA)")
      totalRows(1).monthToTotal(february(2010)) mustEqual Money(102)
      totalRows(1).yearlyAverage mustEqual Money(34)

      summary.totalRowTitles mustEqual Seq(Html("<b>Total</b>"), Html("<b>Total</b> (without catA)"))
    }

    "prunes unused categories" in new WithApplication {
      persistTransaction(groupId = 1, flow = Money(3), date = dateAt(2010, January, 2), category = testCategoryA)

      val summary = Summary.fetchSummary(testAccount, 2010)

      summary.categories must contain(testCategoryA)
      summary.categories must not(contain(testCategoryB))
    }
  }

  // ********** private helper methods ********** //
  private def february(year: Int) = DatedMonth(dateAt(year, February, 1))
}
