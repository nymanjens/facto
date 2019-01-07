package hydro.common

import org.specs2.mutable._

class TagsTest extends Specification {

  "isValidTagName" in {
    Tags.isValidTag("") mustEqual false
    Tags.isValidTag("'") mustEqual false
    Tags.isValidTag("single-illegal-char-at-end?") mustEqual false
    Tags.isValidTag("]single-illegal-char-at-start") mustEqual false

    Tags.isValidTag("a") mustEqual true
    Tags.isValidTag("normal-string") mustEqual true
    Tags.isValidTag("aC29_()_-_@_!") mustEqual true
    Tags.isValidTag("aC29_()_-_@_!_&_$_+_=_._<>_;_:") mustEqual true
  }

  "parseTagsString" in {
    Tags.parseTagsString("a,b,c") mustEqual Seq("a", "b", "c")
    Tags.parseTagsString(",,b,c") mustEqual Seq("b", "c")
    Tags.parseTagsString("") mustEqual Seq()
  }
}
