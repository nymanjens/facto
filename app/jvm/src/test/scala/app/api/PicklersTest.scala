package app.api

import java.time.Month._

import app.api.Picklers._
import app.api.ScalaJsApi._
import app.common.money.Currency
import app.common.testing.TestObjects._
import app.common.testing._
import app.models.accounting.Transaction
import app.models.accounting.config.Config
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.money.ExchangeRateMeasurement
import boopickle.Default._
import boopickle.Pickler
import hydro.common.time.LocalDateTimes
import org.junit.runner._
import org.specs2.runner._

import scala.collection.SortedMap
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class PicklersTest extends HookedSpecification {

  "accounting config" in {
    testPickleAndUnpickle[Config](testAccountingConfig)
  }

  "EntityType" in {
    testPickleAndUnpickle[EntityType.any](ExchangeRateMeasurement.Type)
  }

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionGroupWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testBalanceCheckWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testExchangeRateMeasurementWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testTransactionWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testTransactionGroupWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testBalanceCheckWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testExchangeRateMeasurementWithId))
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
        nextUpdateToken = testUpdateToken
      ))
  }

  "GetAllEntitiesResponse" in {
    testPickleAndUnpickle[GetAllEntitiesResponse](
      GetAllEntitiesResponse(
        entitiesMap = Map(Transaction.Type -> Seq(testTransactionWithId)),
        nextUpdateToken = testUpdateToken))
  }

  "ModificationsWithToken" in {
    testPickleAndUnpickle[ModificationsWithToken](
      ModificationsWithToken(modifications = Seq(testModification), nextUpdateToken = testUpdateToken))
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
