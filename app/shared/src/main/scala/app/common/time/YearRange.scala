package app.common.time

import scala.collection.immutable.Seq

/**
  * A continuous range of years.
  *
  * Example.: YearRange.closed(2014, 2017) consists of the years [2014, 2015, 2016, 2017].
  */
case class YearRange private (private val years: Set[Int]) {
  def firstYear: Int = {
    require(!this.isEmpty)
    years.min
  }

  def lastYear: Int = {
    require(!this.isEmpty)
    years.max
  }

  def toSeq: Seq[Int] = years.toVector.sorted
  def contains(year: Int): Boolean = years contains year
  def isEmpty: Boolean = years.isEmpty
  def nonEmpty: Boolean = years.nonEmpty
  def size: Int = years.size

  def copyIncluding(year: Int): YearRange = year match {
    case _ if isEmpty          => YearRange.single(year)
    case _ if year < firstYear => YearRange.closed(year, lastYear)
    case _ if lastYear < year  => YearRange.closed(firstYear, year)
    case _ if contains(year)   => this
  }

  def copyWithLowerBound(lowerBoundYear: Int): YearRange = YearRange(years.filter(lowerBoundYear <= _))
  def copyLessThan(limitYear: Int): YearRange = YearRange(years.filter(_ < limitYear))
}

object YearRange {
  val empty = YearRange(Set())
  def closed(startYear: Int, endYear: Int): YearRange = {
    require(startYear <= endYear, s"startYear=$startYear should not be later than endYear=$endYear")
    YearRange((startYear to endYear).toSet)
  }
  def single(year: Int): YearRange = YearRange(Set(year))
}
