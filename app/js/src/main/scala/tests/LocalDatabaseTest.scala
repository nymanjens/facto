package tests

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import models.User
import models.access.LocalDatabase
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.accounting.money.ExchangeRateMeasurement
import models.accounting.{BalanceCheck, Transaction, TransactionGroup}
import models.manager.EntityModification
import tests.ManualTests.{ManualTest, ManualTestSuite}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] object LocalDatabaseTest extends ManualTestSuite {

  override def tests = Seq(
    ManualTest("isEmpty") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.isEmpty() ==> true
        db.addAll(Seq(testTransactionWithId))
        db.isEmpty() ==> false

        await(db.clear())

        db.isEmpty() ==> true
        db.setSingletonValue(NextUpdateTokenKey, testDate)
        db.isEmpty() ==> false
      }
    },
    ManualTest("setSingletonValue") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.getSingletonValue(VersionKey).isDefined ==> false

        db.setSingletonValue(VersionKey, "abc")
        db.getSingletonValue(VersionKey).get ==> "abc"

        db.setSingletonValue(NextUpdateTokenKey, testDate)
        db.getSingletonValue(NextUpdateTokenKey).get ==> testDate
      }
    },
    ManualTest("save") {
      async {
        val db = await(LocalDatabase.createStoredForTests())
        await(db.clear())
        db.addAll(Seq(testTransactionWithId))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.save())
        db.setSingletonValue(VersionKey, "otherTestVersion")

        val otherDb = await(LocalDatabase.createStoredForTests())
        otherDb.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
        otherDb.getSingletonValue(VersionKey).get ==> "testVersion"
      }
    },
    ManualTest("clear") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.addAll(Seq(testTransactionWithId))
        db.setSingletonValue(VersionKey, "testVersion")

        await(db.clear())

        db.isEmpty() ==> true
      }
    },
    ManualTest("addAll") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.addAll(Seq(testUser))
        db.addAll(Seq(testTransactionWithId))
        db.addAll(Seq(testTransactionGroupWithId))
        db.addAll(Seq(testBalanceCheckWithId))
        db.addAll(Seq(testExchangeRateMeasurementWithId))

        db.newQuery[User]().data() ==> Seq(testUser)
        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
        db.newQuery[TransactionGroup]().data() ==> Seq(testTransactionGroupWithId)
        db.newQuery[BalanceCheck]().data() ==> Seq(testBalanceCheckWithId)
        db.newQuery[ExchangeRateMeasurement]().data() ==> Seq(testExchangeRateMeasurementWithId)
      }
    },
    ManualTest("addAll: Inserts no duplicates IDs") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transactionWithSameIdA = testTransactionWithId.copy(categoryCode = "codeA")
        val transactionWithSameIdB = testTransactionWithId.copy(categoryCode = "codeB")
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdA))
        db.addAll(Seq(testTransactionWithId, transactionWithSameIdB))

        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
      }
    },
    ManualTest("applyModifications") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        db.addAll(Seq(testTransactionWithId))

        db.applyModifications(
          Seq(
            EntityModification.Add(transaction2),
            EntityModification.createDelete(testTransactionWithId)
          )) ==> true

        db.newQuery[Transaction]().data() ==> Seq(transaction2)
      }
    },
    ManualTest("applyModifications: Is idempotent") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transactionWithSameId = testTransactionWithId.copy(categoryCode = "codeA")
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))

        db.applyModifications(
          Seq(
            EntityModification.Add(testTransactionWithId),
            EntityModification.Add(testTransactionWithId),
            EntityModification.Add(transactionWithSameId),
            EntityModification.Add(transaction2),
            EntityModification.createDelete(transaction2),
            EntityModification.createDelete(transaction2),
            EntityModification.createDelete(transaction3)
          )) ==> true

        db.newQuery[Transaction]().data() ==> Seq(testTransactionWithId)
      }
    },
    ManualTest("applyModifications: Returns false if no change") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.applyModifications(Seq(EntityModification.Add(testTransactionWithId))) ==> true

        db.applyModifications(Seq(EntityModification.Add(testTransactionWithId))) ==> false
      }
    }
  )
}
