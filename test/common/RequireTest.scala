package common

import org.specs2.mutable._
import java.util.{Date, Calendar}
import common.TimeUtils.dateAt

class RequireTest extends Specification {

  "requireNonNullFields" in {
    Require.requireNonNullFields(EmptyTestCaseClass()) // must not throw any exception
    Require.requireNonNullFields(TestCaseClass("", 0)) // must not throw any exception
    Require.requireNonNullFields(TestCaseClass(null, 0)) must
      throwA[IllegalArgumentException].like { case t: Throwable => t.getMessage must contain("fieldA") }
  }

  private case class EmptyTestCaseClass()
  private case class TestCaseClass(fieldA: String, fieldB: Int)
}
