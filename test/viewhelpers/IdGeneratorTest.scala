package viewhelpers

import org.specs2.mutable._

class IdGeneratorTest extends Specification {

  "uniqueId" should {
    "generate distinct ids, even if the suggestion is the same" in {
      val id1 = IdGenerator.uniqueId("abc")
      val id2 = IdGenerator.uniqueId("abc")
      val id3 = IdGenerator.uniqueId("abc")
      val id4 = IdGenerator.uniqueId("def")

      Set(id1, id2, id3, id4) must haveSize(4)
    }

    "remove all special characters from the suggestion" in {
      for (specialCharacter <- List("^", "~", "`", "*", "'", "@", "#", "$")) yield {
        IdGenerator.uniqueId(s"test-$specialCharacter-") must not(contain(specialCharacter))
      }
    }
  }
}