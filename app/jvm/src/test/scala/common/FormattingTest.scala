package common

import com.google.inject._
import common.time.LocalDateTimes.createDateTime
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test._
import java.time.Month._
import common.time._
import common.testing._
import models._
import org.specs2.mutable.Specification
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}

@RunWith(classOf[JUnitRunner])
class FormattingTest extends HookedSpecification {

  @Inject implicit val fakeClock: FakeClock = null
  @Inject implicit val fakeI18n: FakeI18n = null

  override def before() = {
    Guice.createInjector(new FactoTestModule).injectMembers(this)
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
}
