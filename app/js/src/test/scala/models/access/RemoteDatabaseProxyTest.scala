package models.access

import java.time.Month.MARCH

import api.ScalaJsApiClient
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
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Converters._

object RemoteDatabaseProxyTest extends TestSuite {

  override def tests = TestSuite {
    val fakeApiClient: ScalaJsApiClient = new FakeScalaJsApiClient()
    val fakeLocalDatabase: LocalDatabase = new FakeLocalDatabase()

    "name" - {
      val remoteDatabaseProxy = new RemoteDatabaseProxy.Impl(fakeApiClient, Future.successful(fakeLocalDatabase))
      1 ==> 1
    }
  }

  private final class FakeLocalDatabase extends  LocalDatabase{
    // **************** Getters ****************//
    override def newQuery[E <: Entity : EntityType]() = ???
    override def getSingletonValue[V](key: SingletonKey[V]) = ???
    override def isEmpty() = ???

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]) = ???
    override def addAll[E <: Entity : EntityType](entities: Seq[E]) = ???
    override def setSingletonValue[V](key: SingletonKey[V], value: V) = ???
    override def save() = ???
    override def clear() = ???
  }
}
