package api

import api.Picklers._
import api.ScalaJsApi._
import boopickle.Default._
import boopickle.Pickler
import common.testing.TestObjects._
import common.testing._
import models.accounting.Transaction
import models.accounting.config.Config
import models.manager._
import org.junit.runner._
import org.specs2.runner._
import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class PicklersTest extends HookedSpecification {

  "accounting config" in {
    testPickleAndUnpickle[Config](testAccountingConfig)
  }

  "EntityType" in {
    testPickleAndUnpickle[EntityType.any](EntityType.ExchangeRateMeasurementType)
  }

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testTransactionGroupWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testBalanceCheckWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testExchangeRateMeasurementWithId))
    testPickleAndUnpickle[EntityModification](EntityModification.Remove[Transaction](123054))
  }

  "GetInitialDataResponse" in {
    testPickleAndUnpickle[GetInitialDataResponse](
      GetInitialDataResponse(testAccountingConfig, testUserRedacted, i18nMessages = Map("abc" -> "def")))
  }

  "GetAllEntitiesResponse" in {
    testPickleAndUnpickle[GetAllEntitiesResponse](
      GetAllEntitiesResponse(
        entitiesMap = Map(EntityType.TransactionType -> Seq(testTransactionWithId)),
        nextUpdateToken = testDate))
  }

  "GetEntityModificationsResponse" in {
    testPickleAndUnpickle[GetEntityModificationsResponse](
      GetEntityModificationsResponse(modifications = Seq(testModification), nextUpdateToken = testDate))
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
