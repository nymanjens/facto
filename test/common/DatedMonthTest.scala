package common

import org.specs2.mutable._
import common.TimeUtils.{February, dateAt, June, May, April, March}

class DatedMonthTest extends Specification {

  "abbreviation" in {
    val month = DatedMonth(dateAt(1990, June, 1))
    month.abbreviation mustEqual "June"
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
