package common

import java.time.Month._

import com.google.inject._
import common.GuavaReplacement.Splitter
import common.testing._
import common.time.LocalDateTimes.createDateTime
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class GuavaReplacementTest extends HookedSpecification {

  "Splitter" in {
    Splitter.on(' ').split(" a b  c ") mustEqual Seq("", "a", "b", "", "c", "")
    Splitter.on(':').split(" a:b: c :") mustEqual Seq(" a", "b", " c ", "")
    Splitter.on(',').omitEmptyStrings().split(",,,") mustEqual Seq()
    Splitter.on(',').omitEmptyStrings().split(",,a,b") mustEqual Seq("a", "b")
    Splitter.on(',').trimResults().split(" a ,b ") mustEqual Seq("a", "b")
    Splitter.on(',').omitEmptyStrings().trimResults().split(" a ,b ,  ") mustEqual Seq("a", "b")
  }
}
