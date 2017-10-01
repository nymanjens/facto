package flux.stores.entries

import scala.collection.immutable.Seq

/**
  * A continuous range of years.
  *
  * Example.: YearRange(2014, 2017) consists of the years [2014, 2015, 2016].
  */
case class YearRange private (private val startYearInlusive: Int, private val endYearExclusive: Int) {
  def toSeq: Seq[Int] = startYearInlusive until endYearExclusive
  def contains(year: Int): Boolean = startYearInlusive <= year && year < endYearExclusive
}

object YearRange {
  def closed(startYear: Int, endYear: Int): YearRange = YearRange(startYear, endYear + 1)
  def single(year: Int): YearRange = YearRange(year, year + 1)
}
