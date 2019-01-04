package app.common

import app.common.testing._
import org.junit.runner._
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
}
