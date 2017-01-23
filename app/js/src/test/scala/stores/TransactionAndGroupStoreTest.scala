package stores

import java.time.Month.JANUARY
import java.lang.Math.abs
import common.testing.FakeRemoteDatabaseProxy
import common.time.{LocalDateTime, LocalDateTimes}
import common.time.LocalDateTimes.createDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.{EntityModification, EntityType}
import utest._
import common.testing.TestObjects._
import models.User
import models.access.RemoteDatabaseProxy
import stores.LastNEntriesStoreFactory.{LastNEntriesState, N}
import stores.entries.GeneralEntry

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala.util.Random
import scala2js.Converters._

object TransactionAndGroupStoreTest extends TestSuite {

  override def tests = TestSuite {
//    implicit val database = new FakeRemoteDatabaseProxy()
//    val factory: LastNEntriesStoreFactory = new LastNEntriesStoreFactory()
//    val store: EntriesStore[LastNEntriesState] = factory.get(N(3))
//
//    "factory result is cached" - {
//    }
    // TODO
  }
}
