package app.common.time

import scala.collection.immutable.Seq

/**
 * A continuous range of years.
 *
 * Example.: YearRange.closed(AccountingYear(2014), AccountingYear(2017)) consists of the years [2014, 2015, 2016, 2017].
 */
case class YearRange private (private val years: Set[AccountingYear]) {
  def firstYear: AccountingYear = {
    require(!this.isEmpty)
    years.min
  }

  def lastYear: AccountingYear = {
    require(!this.isEmpty)
    years.max
  }

  def toSeq: Seq[AccountingYear] = years.toVector.sorted
  def contains(year: AccountingYear): Boolean = years contains year
  def isEmpty: Boolean = years.isEmpty
  def nonEmpty: Boolean = years.nonEmpty
  def size: Int = years.size

  def copyIncluding(year: AccountingYear): YearRange = year match {
    case _ if isEmpty          => YearRange.single(year)
    case _ if year < firstYear => YearRange.closed(year, lastYear)
    case _ if lastYear < year  => YearRange.closed(firstYear, year)
    case _ if contains(year)   => this
  }

  def copyWithLowerBound(lowerBoundYear: AccountingYear): YearRange = YearRange(
    years.filter(lowerBoundYear <= _)
  )
  def copyLessThan(limitYear: AccountingYear): YearRange = YearRange(years.filter(_ < limitYear))
}

object YearRange {
  val empty: YearRange = YearRange(Set())
  def closed(startYear: AccountingYear, endYear: AccountingYear): YearRange = {
    require(startYear <= endYear, s"startYear=$startYear should not be later than endYear=$endYear")
    YearRange((startYear.startYear to endYear.startYear).map(AccountingYear).toSet)
  }
  def single(year: AccountingYear): YearRange = YearRange(Set(year))
}
