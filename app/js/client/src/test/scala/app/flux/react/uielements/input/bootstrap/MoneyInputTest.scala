package app.flux.react.uielements.input.bootstrap

import app.flux.react.uielements.input.bootstrap.MoneyInput.StringArithmetic
import scala2js.Converters._
import utest._

object MoneyInputTest extends TestSuite {

  override def tests = TestSuite {
    "StringArithmetic" - {
      "floatStringToCents" - {
        "correct arithmetic combinations" - {
          "1 + 1" - { testNameToCents() ==> Some(200) }
          "1.12 + 2.34" - { testNameToCents() ==> Some(346) }

          "1 - 1" - { testNameToCents() ==> Some(0) }
          "1.12 - 2.34" - { testNameToCents() ==> Some(-122) }

          "1   * 1" - { testNameToCents() ==> Some(100) }
          "2*10.1" - { testNameToCents() ==> Some(2020) }
          "20,000 * 2" - { testNameToCents() ==> Some(40 * 1000 * 100) }

          "1/1" - { testNameToCents() ==> Some(100) }
          "1/2" - { testNameToCents() ==> Some(50) }
          "2 / 3" - { testNameToCents() ==> Some(66) }
          "3.14 / 2.71" - { testNameToCents() ==> Some(115) }

          "1 - 1/2 - 2.3 * 2" - { testNameToCents() ==> Some(-410) }
        }

        "empty string" - { StringArithmetic.floatStringToCents("") ==> None }

        "sign prefix" - {
          "+1" - { testNameToCents() ==> Some(100) }
          "-1" - { testNameToCents() ==> Some(-100) }
          " + 1" - { testNameToCents() ==> Some(100) }
        }

        "illegal statements" - {
          "-" - { testNameToCents() ==> None }
          "+" - { testNameToCents() ==> None }
          "*" - { testNameToCents() ==> None }

          "1+" - { testNameToCents() ==> None }
          "1-" - { testNameToCents() ==> None }
          "1*" - { testNameToCents() ==> None }
          "1/" - { testNameToCents() ==> None }

          "*1" - { testNameToCents() ==> None }
          "/1" - { testNameToCents() ==> None }

          "1 - 1/2 - 2.3x" - { testNameToCents() ==> None }
          "1 + " - { testNameToCents() ==> None }
          "1 + x" - { testNameToCents() ==> None }
        }

        "1/0" - { testNameToCents() ==> Some(0) }
      }
    }
  }

  private def testNameToCents()(implicit testPath: utest.framework.TestPath): Option[Long] = {
    val testName = testPath.value.last
    StringArithmetic.floatStringToCents(testName)
  }
}
