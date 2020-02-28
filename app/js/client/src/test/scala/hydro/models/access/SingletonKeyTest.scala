package hydro.models.access

import app.common.testing.TestObjects._
import hydro.models.access.SingletonKey.DbStatus
import hydro.models.access.SingletonKey.DbStatusKey
import hydro.scala2js.Scala2Js
import utest._

import scala.language.reflectiveCalls

object SingletonKeyTest extends TestSuite {
  override def tests = TestSuite {
    "DbStatusKey.valueConverter" - {
      "to JS and back" - {
        testToJsAndBack[DbStatus](DbStatus.Ready)(DbStatusKey.valueConverter)
        testToJsAndBack[DbStatus](DbStatus.Populating(testInstant))(DbStatusKey.valueConverter)
      }
    }
  }

  private def testToJsAndBack[T: Scala2Js.Converter](value: T) = {
    val jsValue = Scala2Js.toJs[T](value)
    val generated = Scala2Js.toScala[T](jsValue)
    generated ==> value
  }
}
