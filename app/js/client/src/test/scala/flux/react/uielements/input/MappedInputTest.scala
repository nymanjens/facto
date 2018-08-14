package flux.react.uielements.input

import java.time.Month.APRIL

import common.testing.TestModule
import common.time.LocalDateTime
import common.time.LocalDateTimes.createDateTime
import scala2js.Converters._
import utest._

object MappedInputTest extends TestSuite {

  override def tests = TestSuite {

    "ValueTransformer.StringToLocalDateTime.forward() works" - {
      val stringToLocalDateTime = MappedInput.ValueTransformer.StringToLocalDateTime
      stringToLocalDateTime.forward("2017-04-03") ==> Some(createDateTime(2017, APRIL, 3))
      stringToLocalDateTime.forward("2017-04-0333") ==> None
    }

    "ValueTransformer.StringToLocalDateTime.backward() works" - {
      val stringToLocalDateTime = MappedInput.ValueTransformer.StringToLocalDateTime
      stringToLocalDateTime.backward(createDateTime(2017, APRIL, 3)) ==> "2017-04-03"
    }
  }
}
