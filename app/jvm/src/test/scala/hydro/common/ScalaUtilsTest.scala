package hydro.common

import org.specs2.mutable._

class ScalaUtilsTest extends Specification {

  "objectName" in {
    ScalaUtils.objectName(TestObject) mustEqual "TestObject"
  }

  object TestObject
}
