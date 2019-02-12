package hydro.common

import hydro.common.testing._
import org.junit.runner._
import org.specs2.matcher.MatchResult
import org.specs2.runner._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class CollectionUtilsTest extends HookedSpecification {

  "getMostCommonString" in {
    CollectionUtils.getMostCommonString(Seq("abc")) mustEqual "abc"
    CollectionUtils.getMostCommonString(Seq("abc", "ABC", "abc", "def")) mustEqual "abc"
    CollectionUtils.getMostCommonString(Seq("abc", "ABC", "ABC", "def")) mustEqual "ABC"
    CollectionUtils.getMostCommonString(Seq("abc", "abc", "ABC", "ABC", "def", "def", "def")) mustEqual "def"
  }

  "getMostCommonStringIgnoringCase" in {
    CollectionUtils.getMostCommonStringIgnoringCase(Seq("abc")) mustEqual "abc"
    CollectionUtils.getMostCommonStringIgnoringCase(Seq("abc", "ABC", "abc", "def")) mustEqual "abc"
    CollectionUtils.getMostCommonStringIgnoringCase(Seq("abc", "ABC", "ABC", "def")) mustEqual "ABC"
    CollectionUtils.getMostCommonStringIgnoringCase(
      Seq("abc", "abc", "ABC", "ABC", "ABC", "def", "def", "def", "def")) mustEqual "ABC"
  }

  "toBiMapWithStableIntKeys" in {
    def doTest[V](stableNameMapper: V => String, a: V, b: V, c: V): MatchResult[_] = {
      val abcMap = CollectionUtils.toBiMapWithStableIntKeys(stableNameMapper, Seq(a, b, c))

      abcMap.keySet mustEqual Set(a, b, c)
      abcMap.inverse().keySet must haveSize(3)

      CollectionUtils.toBiMapWithStableIntKeys(stableNameMapper, Seq(c, a, b)) mustEqual abcMap
      CollectionUtils.toBiMapWithStableIntKeys(stableNameMapper, Seq(a)).get(a) mustEqual abcMap.get(a)
      CollectionUtils.toBiMapWithStableIntKeys(stableNameMapper, Seq(a, a, c)) must throwAn[Exception]
    }

    object A
    object B
    object C

    "with strings" in doTest[String](s => s, "a", "b", "C")
    "with objects" in doTest[Any](_.getClass.getSimpleName, A, B, C)
  }
}
