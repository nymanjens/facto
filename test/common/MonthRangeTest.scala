package common

import org.specs2.mutable._
import common.TimeUtils.{May, June, February, April, March, dateAt}

class MonthRangeTest extends Specification {

  "forYear factory method" in {
    val range = MonthRange.forYear(1998)
    range.start mustEqual dateAt(1998, TimeUtils.January, 1)
    range.startOfNextMonth mustEqual dateAt(1999, TimeUtils.January, 1)
  }

  "completelyBefore" in {
    (MonthRange.lessThan(dateA) completelyBefore MonthRange.atLeast(dateA)) mustEqual true
    (MonthRange.lessThan(dateA) completelyBefore MonthRange.atLeast(dateB)) mustEqual true
    (MonthRange.atLeast(dateA) completelyBefore MonthRange.lessThan(dateB)) mustEqual false
    (MonthRange.forYear(2011) completelyBefore MonthRange.forYear(2012)) mustEqual true
    (MonthRange.forYear(2012) completelyBefore MonthRange.forYear(2012)) mustEqual false
    (MonthRange.forYear(2013) completelyBefore MonthRange.forYear(2012)) mustEqual false
    (MonthRange(dateA, dateB) completelyBefore MonthRange(dateB, dateC)) mustEqual true
    (MonthRange(dateA, dateC) completelyBefore MonthRange(dateB, dateC)) mustEqual false
  }

  "completelyAfter" in {
    (MonthRange.atLeast(dateA) completelyAfter MonthRange.lessThan(dateA)) mustEqual true
    (MonthRange.atLeast(dateB) completelyAfter MonthRange.lessThan(dateA)) mustEqual true
    (MonthRange.atLeast(dateA) completelyAfter MonthRange.lessThan(dateB)) mustEqual false
    (MonthRange.forYear(2013) completelyAfter MonthRange.forYear(2012)) mustEqual true
    (MonthRange.forYear(2011) completelyAfter MonthRange.forYear(2012)) mustEqual false
    (MonthRange.forYear(2012) completelyAfter MonthRange.forYear(2012)) mustEqual false
    (MonthRange(dateB, dateC) completelyAfter MonthRange(dateA, dateB)) mustEqual true
    (MonthRange(dateA, dateC) completelyAfter MonthRange(dateB, dateC)) mustEqual false
  }

  "intersection" in {
    var expectedRange: MonthRange = null

    expectedRange = MonthRange(dateA, dateB)
    (MonthRange.atLeast(dateA) intersection MonthRange.lessThan(dateB)) mustEqual expectedRange

    expectedRange = MonthRange.empty
    (MonthRange.atLeast(dateB) intersection MonthRange.lessThan(dateB)) mustEqual expectedRange

    expectedRange = MonthRange(dateB, dateC)
    (MonthRange(dateB, dateC) intersection MonthRange(dateA, dateD)) mustEqual expectedRange

    expectedRange = MonthRange(dateB, dateC)
    (MonthRange(dateB, dateC) intersection MonthRange(dateB, dateD)) mustEqual expectedRange

    expectedRange = MonthRange(dateB, dateC)
    (MonthRange(dateB, dateC) intersection MonthRange(dateB, dateC)) mustEqual expectedRange

    expectedRange = MonthRange.empty
    (MonthRange(dateB, dateC) intersection MonthRange(dateC, dateD)) mustEqual expectedRange
  }

  "countMonths" in {
    val range = MonthRange(dateAt(1998, TimeUtils.February, 1), dateAt(1999, TimeUtils.June, 1))
    range.countMonths mustEqual 16
  }

  "contains DateTime" in {
    val closedRange = MonthRange(dateA, dateB)

    closedRange.contains(dateAt(2012, February, 27)) mustEqual false
    closedRange.contains(dateA) mustEqual true
    closedRange.contains(dateAt(2012, March, 2)) mustEqual true
    closedRange.contains(dateB) mustEqual false
    closedRange.contains(dateC) mustEqual false

    val openRange = MonthRange.lessThan(dateB)

    openRange.contains(dateA) mustEqual true
    openRange.contains(dateB) mustEqual false
    openRange.contains(dateC) mustEqual false
  }

  "contains DatedMonth" in {
    val closedRange = MonthRange(dateA, dateB)

    closedRange.contains(DatedMonth.containing(dateAt(2012, February, 27))) mustEqual false
    closedRange.contains(DatedMonth.containing(dateA)) mustEqual true
    closedRange.contains(DatedMonth.containing(dateAt(2012, March, 2))) mustEqual true
    closedRange.contains(DatedMonth.containing(dateB)) mustEqual false
    closedRange.contains(DatedMonth.containing(dateC)) mustEqual false

    val openRange = MonthRange.lessThan(dateB)

    openRange.contains(DatedMonth.containing(dateA)) mustEqual true
    openRange.contains(DatedMonth.containing(dateB)) mustEqual false
    openRange.contains(DatedMonth.containing(dateC)) mustEqual false
  }

  val dateA = dateAt(2012, March, 1)
  val dateB = dateAt(2012, April, 1)
  val dateC = dateAt(2012, May, 1)
  val dateD = dateAt(2012, June, 1)
}