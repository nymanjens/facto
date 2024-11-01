package app.common.time

import org.specs2.mutable._

import scala.collection.immutable.Seq

class YearRangeTest extends Specification {

  val y2005 = AccountingYear(2005)
  val y2006 = AccountingYear(2006)
  val y2007 = AccountingYear(2007)
  val y2008 = AccountingYear(2008)
  val y2009 = AccountingYear(2009)
  val y2010 = AccountingYear(2010)
  val y2011 = AccountingYear(2011)
  val y2012 = AccountingYear(2012)

  "closed" in {
    YearRange.closed(y2007, y2010).toSeq mustEqual Seq(y2007, y2008, y2009, y2010)
    YearRange.closed(y2007, y2007).toSeq mustEqual Seq(y2007)
    YearRange.closed(y2009, y2008).toSeq must throwA[IllegalArgumentException]
  }
  "single" in {
    YearRange.single(y2007).toSeq mustEqual Seq(y2007)
  }
  "empty" in {
    YearRange.empty.toSeq mustEqual Seq()
  }
  "firstYear" in {
    YearRange.closed(y2007, y2010).firstYear mustEqual y2007
    YearRange.empty.firstYear must throwA[IllegalArgumentException]
  }
  "lastYear" in {
    YearRange.closed(y2007, y2010).lastYear mustEqual y2010
    YearRange.empty.lastYear must throwA[IllegalArgumentException]
  }
  "contains" in {
    YearRange.closed(y2007, y2010).contains(y2006) mustEqual false
    YearRange.closed(y2007, y2010).contains(y2007) mustEqual true
    YearRange.closed(y2007, y2010).contains(y2008) mustEqual true
    YearRange.closed(y2007, y2010).contains(y2009) mustEqual true
    YearRange.closed(y2007, y2010).contains(y2010) mustEqual true
    YearRange.closed(y2007, y2010).contains(y2011) mustEqual false
    YearRange.empty.contains(y2010) mustEqual false
  }
  "isEmpty" in {
    YearRange.closed(y2007, y2010).isEmpty mustEqual false
    YearRange.empty.isEmpty mustEqual true
  }
  "size" in {
    YearRange.closed(y2007, y2010).size mustEqual 4
    YearRange.single(y2007).size mustEqual 1
    YearRange.empty.size mustEqual 0
  }
  "copyIncluding" in {
    YearRange.closed(y2007, y2010).copyIncluding(y2008) mustEqual YearRange.closed(y2007, y2010)
    YearRange.closed(y2007, y2010).copyIncluding(y2005) mustEqual YearRange.closed(y2005, y2010)
    YearRange.closed(y2007, y2010).copyIncluding(y2012) mustEqual YearRange.closed(y2007, y2012)

    YearRange.single(y2008).copyIncluding(y2008) mustEqual YearRange.single(y2008)
    YearRange.single(y2008).copyIncluding(y2006) mustEqual YearRange.closed(y2006, y2008)
    YearRange.single(y2008).copyIncluding(y2012) mustEqual YearRange.closed(y2008, y2012)

    YearRange.empty.copyIncluding(y2008) mustEqual YearRange.single(y2008)
  }
  "copyWithLowerBound" in {
    YearRange.closed(y2007, y2010).copyWithLowerBound(y2008) mustEqual YearRange.closed(y2008, y2010)
    YearRange.closed(y2007, y2010).copyWithLowerBound(y2005) mustEqual YearRange.closed(y2007, y2010)
    YearRange.closed(y2007, y2010).copyWithLowerBound(y2012) mustEqual YearRange.empty

    YearRange.single(y2008).copyWithLowerBound(y2008) mustEqual YearRange.single(y2008)
    YearRange.single(y2008).copyWithLowerBound(y2006) mustEqual YearRange.single(y2008)
    YearRange.single(y2008).copyWithLowerBound(y2012) mustEqual YearRange.empty

    YearRange.empty.copyWithLowerBound(y2008) mustEqual YearRange.empty
  }
  "copyLessThan" in {
    YearRange.closed(y2007, y2010).copyLessThan(y2008) mustEqual YearRange.single(y2007)
    YearRange.closed(y2007, y2010).copyLessThan(y2005) mustEqual YearRange.empty
    YearRange.closed(y2007, y2010).copyLessThan(y2012) mustEqual YearRange.closed(y2007, y2010)

    YearRange.single(y2008).copyLessThan(y2008) mustEqual YearRange.empty
    YearRange.single(y2008).copyLessThan(y2006) mustEqual YearRange.empty
    YearRange.single(y2008).copyLessThan(y2009) mustEqual YearRange.single(y2008)

    YearRange.empty.copyLessThan(y2008) mustEqual YearRange.empty
  }
}
