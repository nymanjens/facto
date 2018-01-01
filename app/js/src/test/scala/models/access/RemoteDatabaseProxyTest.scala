package models.access

import common.testing.TestObjects._
import common.testing.{FakeScalaJsApiClient, ModificationsBuffer}
import jsfacades.LokiJs
import models.Entity
import models.accounting.Transaction
import models.modification.EntityType.TransactionType
import models.modification.{EntityModification, EntityType}
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._

object RemoteDatabaseProxyTest extends TestSuite {

  override def tests = TestSuite {
    val fakeApiClient: FakeScalaJsApiClient = new FakeScalaJsApiClient()
    val fakeLocalDatabase: FakeLocalDatabase = new FakeLocalDatabase()

    "loads initial data if db is empty" - async {
      fakeApiClient.persistEntityModifications(Seq(testModification))

      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))

      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "loads initial data if db is non-empty but has wrong version" - async {
      fakeLocalDatabase.applyModifications(Seq(testModificationA))
      fakeApiClient.persistEntityModifications(Seq(testModificationB))

      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))

      fakeLocalDatabase.allModifications ==> Seq(testModificationB)
    }

    "does not load initial data if db is non-empty with right version" - async {
      fakeApiClient.persistEntityModifications(Seq(testModificationA))
      val remoteDatabaseProxy1 = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      fakeApiClient.persistEntityModifications(Seq(testModificationB))

      val remoteDatabaseProxy2 = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))

      fakeLocalDatabase.allModifications ==> Seq(testModificationA)
    }

    "newQuery()" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      fakeLocalDatabase.addAll(Seq(testTransactionWithId))

      entityAccess.newQuery[Transaction].data() ==> Seq(testTransactionWithId)
    }

    "hasLocalAddModifications()" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      await(entityAccess.persistModifications(Seq(EntityModification.Add(testTransactionWithId))))

      entityAccess.hasLocalAddModifications(testTransactionWithId) ==> false
    }

    "persistModifications()" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))

      await(entityAccess.persistModifications(Seq(testModification)))

      fakeApiClient.allModifications ==> Seq(testModification)
      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "persistModifications(): calls listeners" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      val listener = new FakeProxyListener()
      entityAccess.registerListener(listener)

      await(entityAccess.persistModifications(Seq(testModification)))

      listener.locallyAdded ==> Seq(Seq(testModification))
      listener.persistedRemotely ==> Seq(Seq(testModification))
      listener.remotelyAdded ==> Seq()
    }

    "clearLocalDatabase()" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      await(entityAccess.persistModifications(Seq(testModification)))

      await(entityAccess.clearLocalDatabase())

      fakeLocalDatabase.isEmpty ==> true
    }

    "updateModifiedEntities()" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      val nextUpdateToken = await(fakeApiClient.getAllEntities(Seq(TransactionType))).nextUpdateToken
      fakeApiClient.persistEntityModifications(Seq(testModification))
      fakeLocalDatabase.allModifications ==> Seq() // sanity check

      await(entityAccess.updateModifiedEntities())

      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "updateModifiedEntities(): calls listeners" - async {
      val entityAccess = await(JsEntityAccess.create(fakeApiClient, fakeLocalDatabase))
      val nextUpdateToken = await(fakeApiClient.getAllEntities(Seq(TransactionType))).nextUpdateToken
      val listener = new FakeProxyListener()
      entityAccess.registerListener(listener)
      fakeApiClient.persistEntityModifications(Seq(testModification))

      await(entityAccess.updateModifiedEntities())

      listener.locallyAdded ==> Seq()
      listener.persistedRemotely ==> Seq()
      listener.remotelyAdded ==> Seq(Seq(testModification))
    }
  }

  private final class FakeLocalDatabase extends LocalDatabase {
    val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
    private val singletonMap: mutable.Map[SingletonKey[_], js.Any] = mutable.Map()

    // **************** Getters ****************//
    override def newQuery[E <: Entity: EntityType]() = {
      DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E]))
    }
    override def getSingletonValue[V](key: SingletonKey[V]) = {
      singletonMap.get(key) map key.valueConverter.toScala
    }
    override def isEmpty = {
      modificationsBuffer.isEmpty && singletonMap.isEmpty
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = {
      modificationsBuffer.addModifications(modifications)
      true
    }
    override def addAll[E <: Entity: EntityType](entities: Seq[E]) = {
      modificationsBuffer.addEntities(entities)
    }
    override def setSingletonValue[V](key: SingletonKey[V], value: V) = {
      singletonMap.put(key, key.valueConverter.toJs(value))
    }
    override def save() = Future.successful((): Unit)
    override def clear() = {
      modificationsBuffer.clear()
      singletonMap.clear()
      Future.successful((): Unit)
    }

    // **************** Additional methods for tests ****************//
    def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()
  }

  private final class FakeProxyListener extends JsEntityAccess.Listener {
    private val _locallyAdded: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()
    private val _persistedRemotely: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()
    private val _remotelyAdded: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()

    override def addedLocally(modifications: Seq[EntityModification]) = {
      _locallyAdded += modifications
    }

    override def localModificationPersistedRemotely(modifications: Seq[EntityModification]) = {
      _persistedRemotely += modifications
    }

    override def addedRemotely(modifications: Seq[EntityModification]) = {
      _remotelyAdded += modifications
    }

    def locallyAdded: Seq[Seq[EntityModification]] = _locallyAdded.toList
    def persistedRemotely: Seq[Seq[EntityModification]] = _persistedRemotely.toList
    def remotelyAdded: Seq[Seq[EntityModification]] = _remotelyAdded.toList
  }
}
