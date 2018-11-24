package api

import java.time.Instant

import api.UpdateTokens.{toInstant, toUpdateToken}
import common.testing.TestUtils._
import common.testing._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class UpdateTokensTest extends HookedSpecification {

  private val time = Instant.now()

  "toLocalDateTime(toUpdateToken())" in {
    toInstant(toUpdateToken(time)) mustEqual time
  }
}
