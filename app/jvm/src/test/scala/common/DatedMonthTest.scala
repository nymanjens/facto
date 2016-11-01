package common

import org.specs2.mutable._
import common.TimeUtils.{April, February, June, March, May, dateAt}
import play.api.i18n.{Lang, Messages}
import play.api.test.{FakeApplication, WithApplication}
import play.api.Application

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.i18n.Messages.Implicits.applicationMessages

class DatedMonthTest extends Specification {

  val application: Application = FakeApplication()
  "abbreviation" in new WithApplication(application) {
    val messages: Messages = applicationMessages(lang = Lang("en"), application)
    val month = DatedMonth(dateAt(1990, June, 1))
    month.abbreviation(messages) mustEqual "June"
  }

  "contains" in {
    val month = DatedMonth(dateAt(1990, June, 1))
    month.contains(dateAt(1990, June, 20)) mustEqual true
    month.contains(dateAt(1990, May, 20)) mustEqual false
    month.contains(dateAt(1991, June, 20)) mustEqual false
  }

  "containing" in {
    val month = DatedMonth.containing(dateAt(1990, June, 8))
    month mustEqual DatedMonth(dateAt(1990, June, 1))
  }
}
