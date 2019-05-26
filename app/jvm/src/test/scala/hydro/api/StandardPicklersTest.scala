package hydro.api

import hydro.models.access.DbQueryImplicits._
import app.api.Picklers._
import app.api.ScalaJsApi._
import app.common.testing.TestObjects._
import app.models.access.ModelFields
import app.models.user.User
import boopickle.Default._
import boopickle.Pickler
import app.api.ScalaJsApi.HydroPushSocketPacket
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken

import hydro.common.testing._
import hydro.models.access.DbQuery
import hydro.models.access.DbQuery.Sorting
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import org.junit.runner._
import org.specs2.runner._

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class StandardPicklersTest extends HookedSpecification {

  "LocalDateTime" in {
    testPickleAndUnpickle(testDate)
  }

  "Instant" in {
    testPickleAndUnpickle(testInstant)
  }

  "EntityType" in {
    testPickleAndUnpickle[EntityType.any](User.Type)
  }

  "LastUpdateTime" in {
    testPickleAndUnpickle(testLastUpdateTime)
  }

  "PicklableDbQuery" in {
    testPickleAndUnpickle(
      PicklableDbQuery.fromRegular(
        DbQuery[User](
          filter = ModelFields.User.loginName === "xxx",
          sorting = Some(Sorting.ascBy(ModelFields.User.loginName)),
          limit = Some(10))))
  }

  "EntityModification" in {
    testPickleAndUnpickle[EntityModification](EntityModification.Add(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Update(testUserRedacted))
    testPickleAndUnpickle[EntityModification](EntityModification.Remove[User](123054))
  }

  "GetAllEntitiesResponse" in {
    testPickleAndUnpickle[GetAllEntitiesResponse](
      GetAllEntitiesResponse(
        entitiesMap = Map(User.Type -> Seq(testUserRedacted)),
        nextUpdateToken = testUpdateToken))
  }

  "ModificationsWithToken" in {
    testPickleAndUnpickle[EntityModificationsWithToken](
      EntityModificationsWithToken(modifications = Seq(testModification), nextUpdateToken = testUpdateToken))
  }
  "VersionCheck" in {
    testPickleAndUnpickle[HydroPushSocketPacket.VersionCheck](
      HydroPushSocketPacket.VersionCheck(versionString = "1.2.3"))
  }

  private def testPickleAndUnpickle[T: Pickler](value: T) = {
    val bytes = Pickle.intoBytes[T](value)
    val unpickled = Unpickle[T].fromBytes(bytes)
    unpickled mustEqual value
  }
}
