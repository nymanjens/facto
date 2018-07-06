package api

import api.UpdateTokens.{toLocalDateTime, toUpdateToken}
import common.testing.TestUtils._
import common.testing._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class UpdateTokensTest extends HookedSpecification {

  private val date = localDateTimeOfEpochSecond(999000111)

  "toLocalDateTime(toUpdateToken())" in {
    toLocalDateTime(toUpdateToken(date)) mustEqual date
  }
}
