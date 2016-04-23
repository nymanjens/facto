package common

import org.specs2.mutable._
import java.util.{Date, Calendar}
import common.TimeUtils.dateAt

class ScalaUtilsTest extends Specification {

  "objectName" in {
    ScalaUtils.objectName(TestObject) mustEqual "TestObject"
  }

  object TestObject
}