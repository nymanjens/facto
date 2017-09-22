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
  }
}
