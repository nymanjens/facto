package hydro.common

import hydro.common.GuavaReplacement.ImmutableSetMultimap
import hydro.common.GuavaReplacement.Splitter
import app.common.testing._
import hydro.common.testing._
import hydro.common.GuavaReplacement.ImmutableBiMap
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

  "ImmutableSetMultimap" in {
    "get" in {
      val multimap =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).put("b", 10).putAll("b", 2, 3).build()

      multimap.get("a") mustEqual Set(1)
      multimap.get("b") mustEqual Set(10, 2, 3)
      multimap.get("c") mustEqual Set()
    }
    "keySet" in {
      val multimap =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).putAll("b", 2, 3).build()

      multimap.keySet mustEqual Set("a", "b")
    }
    "containsValue" in {
      val multimap =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).putAll("b", 2, 3).build()

      multimap.containsValue(1) mustEqual true
      multimap.containsValue(2) mustEqual true
      multimap.containsValue(4) mustEqual false
    }
    "values" in {
      val multimap =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).putAll("b", 2, 3).build()

      multimap.values.toSet mustEqual Set(1, 2, 3)
    }
    "equals" in {
      val multimap1 =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).putAll("b", 2, 3).build()
      val multimap2 =
        ImmutableSetMultimap.builder[String, Int]().put("b", 3).put("b", 2).put("a", 1).build()
      val multimap3 =
        ImmutableSetMultimap.builder[String, Int]().put("a", 1).putAll("b", 2).build()

      multimap1 mustEqual multimap2
      multimap2 mustEqual multimap1
      multimap1 mustNotEqual multimap3
      multimap3 mustNotEqual multimap2
      multimap1 mustNotEqual "some string"
    }
  }

  "ImmutableBiMap" in {
    val biMap = ImmutableBiMap.builder[String, Int]().put("a", 1).put("b", 2).put("c", 3).build()

    "get()" in {
      biMap.get("a") shouldEqual 1
    }
    "inverse()" in {
      biMap.inverse().get(2) shouldEqual "b"
    }
    "keySet()" in {
      biMap.keySet shouldEqual Set("a", "b", "c")
    }
    "equals()" in {
      biMap mustEqual ImmutableBiMap.builder().put("a", 1).put("b", 2).put("c", 3).build()
      biMap mustNotEqual ImmutableBiMap.builder().put("a", 8).put("b", 2).put("c", 3).build()
    }
    "hashCode()" in {
      biMap
        .hashCode() mustEqual ImmutableBiMap.builder().put("a", 1).put("b", 2).put("c", 3).build().hashCode()
    }
    "Throws on duplicate keys" in {
      ImmutableBiMap.builder().put("a", 1).put("a", 2).build() must throwAn[Exception]
    }
    "Throws on duplicate values" in {
      ImmutableBiMap.builder().put("a", 1).put("b", 1).build() must throwAn[Exception]
    }
  }
}
