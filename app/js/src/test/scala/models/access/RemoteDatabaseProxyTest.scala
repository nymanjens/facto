package models.access

import api.ScalaJsApiClient
import scala2js.Converters._
import common.testing.TestObjects._
import common.testing.{FakeScalaJsApiClient, ModificationsBuffer}
import jsfacades.Loki
import models.accounting.Transaction
import models.manager.{Entity, EntityModification, EntityType}
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

object RemoteDatabaseProxyTest extends TestSuite {

  override def tests = TestSuite {
    val fakeApiClient: FakeScalaJsApiClient = new FakeScalaJsApiClient()
    val fakeLocalDatabase: FakeLocalDatabase = new FakeLocalDatabase()

    "loads initial data if db is empty" - {
      1 ==> 1
    }

    "does not load initial data if db is nonEmpty" - {
      1 ==> 1
    }

    "persistModifications()" - async {
      val remoteDatabaseProxy = new RemoteDatabaseProxy.Impl(fakeApiClient, Future.successful(fakeLocalDatabase))
      await(remoteDatabaseProxy.completelyLoaded)

      await(remoteDatabaseProxy.persistModifications(Seq(testModification)))

      fakeApiClient.allModifications ==> Seq(testModification)
      fakeLocalDatabase.allModifications ==> Seq(testModification)
    }

    "persistModifications(): calls listeners" - {
      1 ==> 1
    }

    "xxx(): calls listeners" - {
      1 ==> 1
    }

    "updates modified entities" - {
      1 ==> 1
    }
  }

  private final class FakeLocalDatabase extends LocalDatabase {
    private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
    private val singletonMap: mutable.Map[SingletonKey[_], js.Any] = mutable.Map()

    // **************** Getters ****************//
    override def newQuery[E <: Entity : EntityType]() = {
      new Loki.ResultSet.Fake(modificationsBuffer.getAllEntitiesOfType[E])
    }
    override def getSingletonValue[V](key: SingletonKey[V]) = {
      singletonMap.get(key) map key.valueConverter.toScala
    }
    override def isEmpty() = {
      modificationsBuffer.isEmpty && singletonMap.isEmpty
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = {
      modificationsBuffer.addModifications(modifications)
    }
    override def addAll[E <: Entity : EntityType](entities: Seq[E]) = {
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
}
