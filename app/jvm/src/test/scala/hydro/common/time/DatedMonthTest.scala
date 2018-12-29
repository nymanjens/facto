package hydro.common.time

import java.time.LocalDate
import java.time.Month._

import com.google.inject.Guice
import com.google.inject.Inject
import app.common.testing.TestModule
import app.common.testing.FakeI18n
import app.common.testing.HookedSpecification
import hydro.common.time.LocalDateTimes.createDateTime

class DatedMonthTest extends HookedSpecification {

  @Inject implicit private val fakeI18n: FakeI18n = null

  override def before() = {
    Guice.createInjector(new TestModule).injectMembers(this)
  }

  "abbreviation" in {
    val month = DatedMonth(LocalDate.of(1990, JUNE, 1))
    month.abbreviation mustEqual "app.date.month.jun.abbrev"
  }

  "contains" in {
    val month = DatedMonth(LocalDate.of(1990, JUNE, 1))
    month.contains(createDateTime(1990, JUNE, 20)) mustEqual true
    month.contains(createDateTime(1990, MAY, 20)) mustEqual false
    month.contains(createDateTime(1991, JUNE, 20)) mustEqual false
  }

  "containing" in {
    val month = DatedMonth.containing(LocalDate.of(1990, JUNE, 8))
    month mustEqual DatedMonth(LocalDate.of(1990, JUNE, 1))
  }

  "allMonthsIn" in {
    val months = DatedMonth.allMonthsIn(1990)
    months mustEqual Seq(
      DatedMonth(LocalDate.of(1990, JANUARY, 1)),
      DatedMonth(LocalDate.of(1990, FEBRUARY, 1)),
      DatedMonth(LocalDate.of(1990, MARCH, 1)),
      DatedMonth(LocalDate.of(1990, APRIL, 1)),
      DatedMonth(LocalDate.of(1990, MAY, 1)),
      DatedMonth(LocalDate.of(1990, JUNE, 1)),
      DatedMonth(LocalDate.of(1990, JULY, 1)),
      DatedMonth(LocalDate.of(1990, AUGUST, 1)),
      DatedMonth(LocalDate.of(1990, SEPTEMBER, 1)),
      DatedMonth(LocalDate.of(1990, OCTOBER, 1)),
      DatedMonth(LocalDate.of(1990, NOVEMBER, 1)),
      DatedMonth(LocalDate.of(1990, DECEMBER, 1))
    )
  }

  "startTime" in {
    val month = DatedMonth(LocalDate.of(1990, JUNE, 1))
    month.startTime mustEqual LocalDateTime.of(1990, JUNE, 1, hour = 0, minute = 0)
  }
  "startTimeOfNextMonth" in {
    val month = DatedMonth(LocalDate.of(1990, JUNE, 1))
    month.startTimeOfNextMonth mustEqual LocalDateTime.of(1990, JULY, 1, hour = 0, minute = 0)
  }
}
