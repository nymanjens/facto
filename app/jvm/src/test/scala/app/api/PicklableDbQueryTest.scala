package app.api

import app.api.Picklers._
import app.common.testing._
import hydro.common.time.LocalDateTime
import hydro.models.access.DbQueryImplicits._


import hydro.models.access.DbQuery
import app.models.access.AppDbQuerySorting
import app.models.access.AppDbQuerySorting
import app.models.access.ModelFields
import hydro.models.access.ModelField
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
            DbQuery.Sorting
              .ascBy(ModelFields.Transaction.createdDate)
              .thenDescBy(ModelFields.Transaction.id)),
          limit = Some(192)
        ))
    }
    "filters" in {
      val filters: Seq[DbQuery.Filter[Transaction]] = Seq(
        ModelFields.Transaction.issuerId === 5,
        ModelFields.Transaction.issuerId !== 5,
        (ModelFields.Transaction.issuerId < 5) || (ModelFields.Transaction.createdDate > LocalDateTime.MIN),
        (ModelFields.Transaction.description containsIgnoreCase "abc") && (ModelFields.Transaction.tags contains "abc")
      )
      for (filter <- filters) yield {
        testFromRegularToRegular(DbQuery[Transaction](filter = filter, sorting = None, limit = Some(192)))
      }
    }
  }
}
