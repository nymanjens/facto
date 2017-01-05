package models.access

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import java.time.Month.MARCH

import common.time.LocalDateTime
import models.accounting._
import models.accounting.money.ExchangeRateMeasurement
import models.manager.EntityType
import utest._
import common.testing.TestObjects._
import models.User

import scala.collection.immutable.Seq
import scala.scalajs.js
import scala2js.Converters._

object LocalDatabaseTest extends TestSuite {

  override def tests = TestSuite {
    "isEmpty" - {
      //      LocalDatabase.createInMemoryForTests() map { db =>
      //        db.isEmpty() ==> true
      //        db.addAll(Seq(testTransactionWithId))
      //        db.isEmpty() ==> false
      //      }

      LocalDatabase.createInMemoryForTests()
    }
  }
}
