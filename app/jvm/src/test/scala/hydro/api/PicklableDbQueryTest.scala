package hydro.api

import app.common.testing._
import hydro.common.testing._
import app.models.access.ModelFields
import app.models.user.User
import hydro.models.access.DbQuery
import hydro.models.access.DbQueryImplicits._
import org.junit.runner._
import org.specs2.runner._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class PicklableDbQueryTest extends HookedSpecification {

  "regular -> picklable -> regular" in {
    def testFromRegularToRegular(query: DbQuery[_]) = {
      PicklableDbQuery.fromRegular(query).toRegular mustEqual query
    }

    "null object" in {
      testFromRegularToRegular(
        DbQuery[User](filter = DbQuery.Filter.NullFilter(), sorting = None, limit = None))
    }
    "limit" in {
      testFromRegularToRegular(
        DbQuery[User](
          filter = DbQuery.Filter.NullFilter(),
          sorting = None,
          limit = Some(192)
        ))
    }
    "filters" in {
      val filters: Seq[DbQuery.Filter[User]] = Seq(
        (ModelFields.User.loginName === "a") && (ModelFields.User.loginName !== "b"),
        ModelFields.User.name containsIgnoreCase "abc"
      )
      for (filter <- filters) yield {
        testFromRegularToRegular(DbQuery[User](filter = filter, sorting = None, limit = Some(192)))
      }
    }
  }
}
