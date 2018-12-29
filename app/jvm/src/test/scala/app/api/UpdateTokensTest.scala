package app.api

import java.time.Instant

import app.api.UpdateTokens.toInstant
import app.api.UpdateTokens.toUpdateToken
import app.common.testing.TestUtils._
import app.common.testing._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class UpdateTokensTest extends HookedSpecification {

  private val time = Instant.now()

  "toLocalDateTime(toUpdateToken())" in {
    toInstant(toUpdateToken(time)) mustEqual time
  }
}
