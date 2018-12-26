package app.api

import app.api.Picklers._
import common.testing._
import hydro.common.time.LocalDateTime
import app.models.access.DbQueryImplicits._
import app.models.access.DbQuery
import app.models.access.ModelField
import app.models.accounting.Transaction
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
        DbQuery[Transaction](filter = DbQuery.Filter.NullFilter(), sorting = None, limit = None))
    }
    "sorting and limit" in {
      testFromRegularToRegular(
        DbQuery[Transaction](
          filter = DbQuery.Filter.NullFilter(),
          sorting = Some(
            DbQuery.Sorting.ascBy(ModelField.Transaction.createdDate).thenDescBy(ModelField.Transaction.id)),
          limit = Some(192)
        ))
    }
    "filters" in {
      val filters: Seq[DbQuery.Filter[Transaction]] = Seq(
        ModelField.Transaction.issuerId === 5,
        ModelField.Transaction.issuerId !== 5,
        (ModelField.Transaction.issuerId < 5) || (ModelField.Transaction.createdDate > LocalDateTime.MIN),
        (ModelField.Transaction.description containsIgnoreCase "abc") && (ModelField.Transaction.tags contains "abc")
      )
      for (filter <- filters) yield {
        testFromRegularToRegular(DbQuery[Transaction](filter = filter, sorting = None, limit = Some(192)))
      }
    }
  }
}
