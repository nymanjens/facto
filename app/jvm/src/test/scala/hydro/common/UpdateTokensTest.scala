package hydro.common

import java.time.Instant

import hydro.common.UpdateTokens.toInstant
import hydro.common.UpdateTokens.toUpdateToken
import app.common.testing._
import hydro.common.testing._
import org.junit.runner._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class UpdateTokensTest extends HookedSpecification {

  private val time = Instant.now()

  "toLocalDateTime(toUpdateToken())" in {
    toInstant(toUpdateToken(time)) mustEqual time
  }
}
