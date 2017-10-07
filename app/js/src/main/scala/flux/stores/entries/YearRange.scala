package flux.stores.entries

import scala.collection.immutable.Seq

/**
  * A continuous range of years.
  *
  * Example.: YearRange(2014, 2017) consists of the years [2014, 2015, 2016].
  */
case class YearRange private (private val startYearInlusive: Int, private val endYearExclusive: Int) {
  def firstYear: Int = {
    require(!this.isEmpty)
    startYearInlusive
  }

  def lastYear: Int = {
    require(!this.isEmpty)
    endYearExclusive - 1
  }

  def toSeq: Seq[Int] = startYearInlusive until endYearExclusive
  def contains(year: Int): Boolean = startYearInlusive <= year && year < endYearExclusive
  def isEmpty: Boolean = this == YearRange.empty

  def copyIncluding(year: Int): YearRange = year match {
    case _ if this.isEmpty => YearRange.single(year)
    case _ if this contains year => this
    case _ if year < startYearInlusive => YearRange(year, endYearExclusive)
    case _ if year >= endYearExclusive => YearRange(startYearInlusive, year)
  }

  def copyWithLowerBound(year: Int): YearRange = year match {
    case _ if this.isEmpty => this
    case _ if year <= startYearInlusive => this
    case _ if year < endYearExclusive => YearRange(year, endYearExclusive)
    case _ if year >= endYearExclusive => YearRange.empty
  }
}

object YearRange {
  val empty = YearRange(0, 0)
  def closed(startYear: Int, endYear: Int): YearRange = {
    require(startYear < endYear)
    YearRange(startYear, endYear + 1)
  }
  def single(year: Int): YearRange = YearRange(year, year + 1)
}
