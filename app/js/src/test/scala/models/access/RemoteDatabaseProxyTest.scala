package models.access

import java.time.Month.MARCH

import api.ScalaJsApiClient
import common.testing.{FakeScalaJsApiClient, ModificationsBuffer}
import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{Entity, EntityModification, EntityType}
import utest._
import common.testing.TestObjects._
import jsfacades.Loki
import jsfacades.Loki.ResultSet
import models.User

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Converters._

object RemoteDatabaseProxyTest extends TestSuite {

  override def tests = TestSuite {
    val fakeApiClient: ScalaJsApiClient = new FakeScalaJsApiClient()
    val fakeLocalDatabase: LocalDatabase = new FakeLocalDatabase()

    "loads initial data if db is empty" - {
      1 ==> 1
    }

    "does not load initial data if db is nonEmpty" - {
      1 ==> 1
    }

    "persistModifications" - {
      val remoteDatabaseProxy = new RemoteDatabaseProxy.Impl(fakeApiClient, Future.successful(fakeLocalDatabase))
      1 ==> 1
    }

    "calls listeners" - {
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
  }
}
