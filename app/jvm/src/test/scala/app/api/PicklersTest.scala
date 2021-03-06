package app.api

import java.time.Month._

import app.api.Picklers._
import app.api.ScalaJsApi._
import app.common.money.Currency
import app.common.testing.TestObjects._
import app.models.accounting.Transaction
import app.models.accounting.config.Config
import boopickle.Default._
import boopickle.Pickler
import hydro.common.testing._
import hydro.common.time.LocalDateTimes
import hydro.models.modification.EntityModification
import org.junit.runner._
import org.specs2.runner._

import scala.collection.SortedMap
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class PicklersTest extends HookedSpecification {

  "accounting config" in {
    testPickleAndUnpickle[Config](testAccountingConfig)
  }

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionGroupWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testBalanceCheckWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testExchangeRateMeasurementWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Remove[Transaction](123054))
  }

  "GetInitialDataResponse" in {
    testPickleAndUnpickle[GetInitialDataResponse](
      GetInitialDataResponse(
        accountingConfig = testAccountingConfig,
        user = testUserRedacted,
        allUsers = Seq(testUserRedacted),
        i18nMessages = Map("abc" -> "def"),
        ratioReferenceToForeignCurrency =
          Map(Currency.Gbp -> SortedMap(LocalDateTimes.createDateTime(2012, MAY, 2) -> 1.2349291837)),
        nextUpdateToken = testUpdateToken,
      )
    )
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
