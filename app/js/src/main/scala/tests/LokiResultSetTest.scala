package tests

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import jsfacades.Loki
import models.access.LocalDatabase
import models.accounting.Transaction
import models.manager.EntityModification
import tests.ManualTests.{ManualTest, ManualTestSuite}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._
import scala2js.Keys

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] object LokiResultSetTest extends ManualTestSuite {

  override def tests = Seq(
    ManualTest("newQuery(): Lookup by ID works") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(testTransactionWithId, transaction2, transaction3))

        db.newQuery[Transaction]().findOne(Keys.id , 99992L) ==> Some(transaction2)
      }
    },
    ManualTest("newQuery(): Lookup with lessThan filter") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1)
        val transaction2 = persistTransaction(day = 2)
        val transaction3 = persistTransaction(day = 3)

        val data = db
          .newQuery[Transaction]()
          .filterLessThan(Keys.Transaction.createdDate , transaction3.createdDate)
          .sort(Loki.Sorting.ascBy("createdDate"))
          .data()

        data ==> Vector(transaction1, transaction2)
      }
    },
    ManualTest("newQuery(): Lookup with greaterThan filter") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1)
        val transaction2 = persistTransaction(day = 2)
        val transaction3 = persistTransaction(day = 3)

        val data = db
          .newQuery[Transaction]()
          .filterGreaterThan(Keys.Transaction.createdDate , transaction1.createdDate)
          .sort(Loki.Sorting.ascBy("createdDate"))
          .data()

        data ==> Vector(transaction2, transaction3)
      }
    },
    ManualTest("newQuery(): Sorts values") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(groupId = 1, day = 2)
        val transaction2 = persistTransaction(groupId = 1, day = 3)
        val transaction3 = persistTransaction(groupId = 2, day = 1)

        val data = db
          .newQuery[Transaction]()
          .sort(Loki.Sorting.descBy("groupId").thenAscBy("createdDate"))
          .data()

        data ==> Vector(transaction3, transaction1, transaction2)
      }
    }
  )

  private def persistTransaction(groupId: Long = 1, day: Int = 1)(implicit db: LocalDatabase): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(EntityModification.generateRandomId()),
      transactionGroupId = groupId,
      createdDate = createDateTime(2012, JANUARY, day),
      transactionDate = createDateTime(2012, JANUARY, day),
      consumedDate = createDateTime(2012, JANUARY, day)
    )
    db.addAll(Seq(transaction))
    transaction
  }
}
