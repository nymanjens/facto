package models.access

import java.time.Month.MARCH

import api.ScalaJsApiClient
import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.EntityType
import utest._
import common.testing.TestObjects._
import models.User

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.js
import scala2js.Converters._

object RemoteDatabaseProxyTest extends TestSuite {

  override def tests = TestSuite {
    val fakeApiClient: ScalaJsApiClient = new FakeScalaJsApiClient()
    val localDatabase: LocalDatabase =

    "name" - {
      val remoteDatabaseProxy = new RemoteDatabaseProxy.Impl(fakeApiClient, Future.successful(fakeLocalDatabase))
      1 ==> 1
    }
  }

  private final class FakeLocalDatabase extends  LocalDatabase{

  }
}
