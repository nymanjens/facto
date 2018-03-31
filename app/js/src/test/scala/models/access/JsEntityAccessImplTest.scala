package models.access

import common.testing.TestObjects._
import common.testing.{FakeScalaJsApiClient, ModificationsBuffer, TestModule}
import common.time.Clock
import jsfacades.LokiJs
import models.Entity
import models.accounting.Transaction
import models.modification.EntityType.TransactionType
import models.modification.{EntityModification, EntityType}
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._

object JsEntityAccessImplTest extends TestSuite {

  override def tests = TestSuite {
    implicit val fakeApiClient: FakeScalaJsApiClient = new FakeScalaJsApiClient()
    implicit val fakeClock: Clock = new TestModule().fakeClock
    val fakeLocalDatabase: FakeLocalDatabase = new FakeLocalDatabase()
    val localDatabasePromise: Promise[LocalDatabase] = Promise()
    implicit val remoteDatabaseProxy: RemoteDatabaseProxy =
      HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
    val entityAccess = new JsEntityAccessImpl(allUsers = Seq())

    "loads initial data if db is empty" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      fakeApiClient.persistEntityModifications(Seq(testModification))

      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "loads initial data if db is non-empty but has wrong version" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      fakeLocalDatabase.applyModifications(Seq(testModificationA))
      fakeApiClient.persistEntityModifications(Seq(testModificationB))

      fakeLocalDatabase.allModifications ==> Seq(testModificationB)
    }

    "does not load initial data if db is non-empty with right version" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      fakeApiClient.persistEntityModifications(Seq(testModificationA))

      val entityAccess1 = {
        implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
        new JsEntityAccessImpl(allUsers = Seq())
      }
      fakeApiClient.persistEntityModifications(Seq(testModificationB))

      val entityAccess2 = {
        implicit val remoteDatabaseProxy = HybridRemoteDatabaseProxy.create(localDatabasePromise.future)
        new JsEntityAccessImpl(allUsers = Seq())
      }

      fakeLocalDatabase.allModifications ==> Seq(testModificationA)
    }

    "newQuery()" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      fakeLocalDatabase.addAll(Seq(testTransactionWithId))

      await(entityAccess.newQuery[Transaction].data()) ==> Seq(testTransactionWithId)
    }

    "hasLocalAddModifications()" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      await(entityAccess.persistModifications(Seq(EntityModification.Add(testTransactionWithId))))

      entityAccess.hasLocalAddModifications(testTransactionWithId) ==> false
    }

    "persistModifications()" - async {
      localDatabasePromise.success(fakeLocalDatabase)

      await(entityAccess.persistModifications(Seq(testModification)))

      fakeApiClient.allModifications ==> Seq(testModification)
      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "persistModifications(): calls listeners" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      val listener = new FakeProxyListener()
      entityAccess.registerListener(listener)

      await(entityAccess.persistModifications(Seq(testModification)))

      listener.modifications ==> Seq(Seq(testModification))
    }

    "updateModifiedEntities()" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      val nextUpdateToken = await(fakeApiClient.getAllEntities(Seq(TransactionType))).nextUpdateToken
      fakeApiClient.persistEntityModifications(Seq(testModification))
      fakeLocalDatabase.allModifications ==> Seq() // sanity check

      await(entityAccess.updateModifiedEntities(None))

      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "updateModifiedEntities(): calls listeners" - async {
      localDatabasePromise.success(fakeLocalDatabase)
      val nextUpdateToken = await(fakeApiClient.getAllEntities(Seq(TransactionType))).nextUpdateToken
      val listener = new FakeProxyListener()
      entityAccess.registerListener(listener)
      fakeApiClient.persistEntityModifications(Seq(testModification))

      await(entityAccess.updateModifiedEntities(None))

      listener.modifications ==> Seq(Seq(testModification))
    }
  }

  private final class FakeLocalDatabase extends LocalDatabase {
    val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
    private val singletonMap: mutable.Map[SingletonKey[_], js.Any] = mutable.Map()

    // **************** Getters ****************//
    override def queryExecutor[E <: Entity: EntityType]() = {
      DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E]).asAsync
    }
    override def getSingletonValue[V](key: SingletonKey[V]) = {
      Future.successful(singletonMap.get(key) map key.valueConverter.toScala)
    }
    override def isEmpty = {
      Future.successful(modificationsBuffer.isEmpty && singletonMap.isEmpty)
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = {
      modificationsBuffer.addModifications(modifications)
      Future.successful(true)
    }
    override def addAll[E <: Entity: EntityType](entities: Seq[E]) = {
      modificationsBuffer.addEntities(entities)
      Future.successful((): Unit)
    }
    override def setSingletonValue[V](key: SingletonKey[V], value: V) = {
      singletonMap.put(key, key.valueConverter.toJs(value))
      Future.successful((): Unit)
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
    private val _modifications: mutable.Buffer[Seq[EntityModification]] = mutable.Buffer()

    override def modificationsAdded(modifications: Seq[EntityModification]) = {
      _modifications += modifications
    }

    def modifications: Seq[Seq[EntityModification]] = _modifications.toVector
  }
}
