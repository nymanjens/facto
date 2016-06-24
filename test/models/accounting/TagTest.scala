package models.accounting

import org.specs2.mutable._
import play.api.test.WithApplication

class TagTest extends Specification {

  "isValidTagName" in new WithApplication {
    Tag.isValidTagName("") mustEqual false
    Tag.isValidTagName("'") mustEqual false
    Tag.isValidTagName("single-illegal-char-at-end?") mustEqual false
    Tag.isValidTagName("]single-illegal-char-at-start") mustEqual false
    Tag.isValidTagName("space in middle") mustEqual false

    Tag.isValidTagName("a") mustEqual true
    Tag.isValidTagName("normal-string") mustEqual true
    Tag.isValidTagName("aC29_()_-_@_!") mustEqual true
    Tag.isValidTagName("aC29_()_-_@_!_&_$_+_=_._<>_;_:") mustEqual true
  }
}
