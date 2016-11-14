package controllers.helpers.accounting

import com.google.inject._
import common.testing._
import scala.collection.immutable.Seq
import scala.collection.JavaConverters._
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import play.twirl.api.Html
import org.joda.time.DateTime
import common.{Clock, DatedMonth, MonthRange, TimeUtils}
import common.TimeUtils.{April, February, January, March, May, dateAt}
import common.testing.TestObjects._
import common.testing.TestUtils._
import common.testing.HookedSpecification
import models.accounting._
import models.accounting.config.{Account, Category, Config}
import models.accounting.money.{Money, ReferenceMoney}

@RunWith(classOf[JUnitRunner])
class SummaryTest extends HookedSpecification {

  @Inject val summaries: Summaries = null

  override def before = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)

    Clock.setTimeForTest(dateAt(2010, April, 4))
  }

  override def afterAll = Clock.cleanupAfterTest()

  "Summary.fetchSummary()" should {
    "caculate monthRangeForAverages" in new WithApplication {
      persistTransaction(flowInCents = 200, date = dateAt(2009, February, 2))
      persistTransaction(flowInCents = 201, date = dateAt(2009, February, 20))
      persistTransaction(flowInCents = 202, date = dateAt(2009, March, 1))
      persistTransaction(flowInCents = 202, date = dateAt(2010, May, 4))

      val summary = summaries.fetchSummary(testAccount, 2009)

      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2009, February, 1), dateAt(2010, April, 1))
    }

    "return successfully when there are no transactions" in new WithApplication {
      val summary = summaries.fetchSummary(testAccount, 2009)
      summary.totalRowTitles must not(beEmpty)
    }

    "ignore the current and future months when calculating the averages" in new WithApplication {
      persistTransaction(flowInCents = 999, date = dateAt(2009, April, 2))
      persistTransaction(flowInCents = 100, date = dateAt(2010, February, 2))
      persistTransaction(flowInCents = 112, date = dateAt(2010, March, 2))
      persistTransaction(flowInCents = 120, date = dateAt(2010, April, 2))
      persistTransaction(flowInCents = 130, date = dateAt(2010, May, 2))
      persistTransaction(flowInCents = 1999, date = dateAt(2011, April, 2))

      val summary = summaries.fetchSummary(testAccount, 2009)

      summary.yearToSummary(2010).categoryToAverages(testCategory) mustEqual ReferenceMoney(71)
      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2009, April, 1), dateAt(2010, April, 1))
    }


    "ignore the pre-facto months when calculating the averages" in new WithApplication {
      persistTransaction(flowInCents = 100, date = dateAt(2010, February, 2))
      persistTransaction(flowInCents = 112, date = dateAt(2010, March, 2))
      persistTransaction(flowInCents = 120, date = dateAt(2010, April, 2))

      val summary = summaries.fetchSummary(testAccount, 2009)

      summary.yearToSummary(2010).categoryToAverages(testCategory) mustEqual ReferenceMoney(106)
      summary.monthRangeForAverages shouldEqual MonthRange(dateAt(2010, February, 1), dateAt(2010, April, 1))
    }

    "calculates totals" in new WithApplication {
      persistTransaction(flowInCents = 3, date = dateAt(2010, January, 2), category = testCategoryA)
      persistTransaction(flowInCents = 100, date = dateAt(2010, February, 2), category = testCategoryA)
      persistTransaction(flowInCents = 102, date = dateAt(2010, February, 2), category = testCategoryB)

      val summary = summaries.fetchSummary(testAccount, 2009)

      val totalRows = summary.yearToSummary(2010).totalRows
      totalRows must haveSize(2)
      totalRows(0).rowTitleHtml mustEqual Html("<b>Total</b>")
      totalRows(0).monthToTotal(february(2010)) mustEqual ReferenceMoney(202)
      totalRows(0).yearlyAverage mustEqual ReferenceMoney(68)
      totalRows(1).rowTitleHtml mustEqual Html("<b>Total</b> (without catA)")
      totalRows(1).monthToTotal(february(2010)) mustEqual ReferenceMoney(102)
      totalRows(1).yearlyAverage mustEqual ReferenceMoney(34)

      summary.totalRowTitles mustEqual Seq(Html("<b>Total</b>"), Html("<b>Total</b> (without catA)"))
    }

    "prunes unused categories" in new WithApplication {
      persistTransaction(flowInCents = 3, date = dateAt(2010, January, 2), category = testCategoryA)

      val summary = summaries.fetchSummary(testAccount, 2010)

      summary.categories must contain(testCategoryA)
      summary.categories must not(contain(testCategoryB))
    }
  }

  // ********** private helper methods ********** //
  private def february(year: Int) = DatedMonth(dateAt(year, February, 1))
}
