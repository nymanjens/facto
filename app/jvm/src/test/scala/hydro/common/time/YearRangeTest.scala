package hydro.common.time

import org.specs2.mutable._

import scala.collection.immutable.Seq

class YearRangeTest extends Specification {

  "closed" in {
    YearRange.closed(2007, 2010).toSeq mustEqual Seq(2007, 2008, 2009, 2010)
    YearRange.closed(2007, 2007).toSeq mustEqual Seq(2007)
    YearRange.closed(2009, 2008).toSeq must throwA[IllegalArgumentException]
  }
  "single" in {
    YearRange.single(2007).toSeq mustEqual Seq(2007)
  }
  "empty" in {
    YearRange.empty.toSeq mustEqual Seq()
  }
  "firstYear" in {
    YearRange.closed(2007, 2010).firstYear mustEqual 2007
    YearRange.empty.firstYear must throwA[IllegalArgumentException]
  }
  "lastYear" in {
    YearRange.closed(2007, 2010).lastYear mustEqual 2010
    YearRange.empty.lastYear must throwA[IllegalArgumentException]
  }
  "contains" in {
    YearRange.closed(2007, 2010).contains(2006) mustEqual false
    YearRange.closed(2007, 2010).contains(2007) mustEqual true
    YearRange.closed(2007, 2010).contains(2008) mustEqual true
    YearRange.closed(2007, 2010).contains(2009) mustEqual true
    YearRange.closed(2007, 2010).contains(2010) mustEqual true
    YearRange.closed(2007, 2010).contains(2011) mustEqual false
    YearRange.empty.contains(2010) mustEqual false
  }
  "isEmpty" in {
    YearRange.closed(2007, 2010).isEmpty mustEqual false
    YearRange.empty.isEmpty mustEqual true
  }
  "size" in {
    YearRange.closed(2007, 2010).size mustEqual 4
    YearRange.single(2007).size mustEqual 1
    YearRange.empty.size mustEqual 0
  }
  "copyIncluding" in {
    YearRange.closed(2007, 2010).copyIncluding(2008) mustEqual YearRange.closed(2007, 2010)
    YearRange.closed(2007, 2010).copyIncluding(2005) mustEqual YearRange.closed(2005, 2010)
    YearRange.closed(2007, 2010).copyIncluding(2012) mustEqual YearRange.closed(2007, 2012)

    YearRange.single(2008).copyIncluding(2008) mustEqual YearRange.single(2008)
    YearRange.single(2008).copyIncluding(2006) mustEqual YearRange.closed(2006, 2008)
    YearRange.single(2008).copyIncluding(2012) mustEqual YearRange.closed(2008, 2012)

    YearRange.empty.copyIncluding(2008) mustEqual YearRange.single(2008)
  }
  "copyWithLowerBound" in {
    YearRange.closed(2007, 2010).copyWithLowerBound(2008) mustEqual YearRange.closed(2008, 2010)
    YearRange.closed(2007, 2010).copyWithLowerBound(2005) mustEqual YearRange.closed(2007, 2010)
    YearRange.closed(2007, 2010).copyWithLowerBound(2012) mustEqual YearRange.empty

    YearRange.single(2008).copyWithLowerBound(2008) mustEqual YearRange.single(2008)
    YearRange.single(2008).copyWithLowerBound(2006) mustEqual YearRange.single(2008)
    YearRange.single(2008).copyWithLowerBound(2012) mustEqual YearRange.empty

    YearRange.empty.copyWithLowerBound(2008) mustEqual YearRange.empty
  }
  "copyLessThan" in {
    YearRange.closed(2007, 2010).copyLessThan(2008) mustEqual YearRange.single(2007)
    YearRange.closed(2007, 2010).copyLessThan(2005) mustEqual YearRange.empty
    YearRange.closed(2007, 2010).copyLessThan(2012) mustEqual YearRange.closed(2007, 2010)

    YearRange.single(2008).copyLessThan(2008) mustEqual YearRange.empty
    YearRange.single(2008).copyLessThan(2006) mustEqual YearRange.empty
    YearRange.single(2008).copyLessThan(2009) mustEqual YearRange.single(2008)

    YearRange.empty.copyLessThan(2008) mustEqual YearRange.empty
  }
}
