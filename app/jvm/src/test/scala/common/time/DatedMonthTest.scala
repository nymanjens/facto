package common.time

import common.time.LocalDateTimes.createDateTime
import org.specs2.mutable._
import java.time.Month._
import play.api.i18n.{Lang, Messages}
import play.api.test.{FakeApplication, WithApplication}
import play.api.Application

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.i18n.Messages.Implicits.applicationMessages

class DatedMonthTest extends Specification {

  val application: Application = FakeApplication()
  "abbreviation" in new WithApplication(application) {
    val messages: Messages = applicationMessages(lang = Lang("en"), application)
    val month = DatedMonth(createDateTime(1990, JUNE, 1))
    month.abbreviation(messages) mustEqual "June"
  }

  "contains" in {
    val month = DatedMonth(createDateTime(1990, JUNE, 1))
    month.contains(createDateTime(1990, JUNE, 20)) mustEqual true
    month.contains(createDateTime(1990, MAY, 20)) mustEqual false
    month.contains(createDateTime(1991, JUNE, 20)) mustEqual false
  }

  "containing" in {
    val month = DatedMonth.containing(createDateTime(1990, JUNE, 8))
    month mustEqual DatedMonth(createDateTime(1990, JUNE, 1))
  }
}
