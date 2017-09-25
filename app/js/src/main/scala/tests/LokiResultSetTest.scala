package tests

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import jsfacades.LokiJs
import models.access.LocalDatabase
import models.accounting.Transaction
import models.accounting.config.Category
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
    ManualTest("newQuery().filter()") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(testTransactionWithId, transaction2, transaction3))

        db.newQuery[Transaction]().filter(Keys.id, 99992L).data() ==> Seq(transaction2)
      }
    },
    ManualTest("newQuery().filterNot()") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction2 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(transaction1, transaction2))

        db.newQuery[Transaction]().filterNot(Keys.id, 99992L).data() ==> Vector(transaction2)
      }
    },
    ManualTest("newQuery().filterLessThan()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1)
        val transaction2 = persistTransaction(day = 2)
        val transaction3 = persistTransaction(day = 3)

        val data = db
          .newQuery[Transaction]()
          .filterLessThan(Keys.Transaction.createdDate, transaction3.createdDate)
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .data()

        data ==> Vector(transaction1, transaction2)
      }
    },
    ManualTest("newQuery().filterGreaterThan()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1)
        val transaction2 = persistTransaction(day = 2)
        val transaction3 = persistTransaction(day = 3)

        val data = db
          .newQuery[Transaction]()
          .filterGreaterThan(Keys.Transaction.createdDate, transaction1.createdDate)
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .data()

        data ==> Vector(transaction2, transaction3)
      }
    },
    ManualTest("newQuery().filterAnyOf()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1, category = testCategoryA)
        val transaction2 = persistTransaction(day = 2, category = testCategoryB)
        persistTransaction(day = 3, category = testCategoryC)

        val data = db
          .newQuery[Transaction]()
          .filterAnyOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code))
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .data()

        data ==> Vector(transaction1, transaction2)
      }
    },
    ManualTest("newQuery().filterNoneOf()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        persistTransaction(day = 1, category = testCategoryA)
        persistTransaction(day = 2, category = testCategoryB)
        val transaction3 = persistTransaction(day = 3, category = testCategoryC)

        val data = db
          .newQuery[Transaction]()
          .filterNoneOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code))
          .data()

        data ==> Vector(transaction3)
      }
    },
    ManualTest("newQuery().filterContainsIgnoreCase()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1, description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        val transaction2 = persistTransaction(day = 2, description = "BBBB.*cccc")
        persistTransaction(day = 3, description = "prefix\nBBBBcccc\nsuffix")

        val data = db
          .newQuery[Transaction]()
          .filterContainsIgnoreCase(Keys.Transaction.description, "BBBB.*cccc")
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .data()

        data ==> Vector(transaction1, transaction2)
      }
    },
    ManualTest("newQuery().filterDoesntContainIgnoreCase()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        persistTransaction(day = 1, description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        persistTransaction(day = 2, description = "BBBB.*cccc")
        val transaction3 = persistTransaction(day = 3, description = "prefix\nBBBBcccc\nsuffix")

        val data = db
          .newQuery[Transaction]()
          .filterDoesntContainIgnoreCase(Keys.Transaction.description, "BBBB.*cccc")
          .data()

        data.map(_.description) ==> Vector(transaction3.description)
        data ==> Vector(transaction3)
      }
    },
    ManualTest("newQuery().filterSeqContains()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1, tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = persistTransaction(day = 2, tags = Seq("tagA", "tagB"))
        val transaction3 = persistTransaction(day = 3, tags = Seq("tag"))

        val data = db
          .newQuery[Transaction]()
          .filterSeqContains(Keys.Transaction.tags, "tag")
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .data()

        data.map(_.tags) ==> Vector(transaction1.tags,transaction3.tags)
        data ==> Vector(transaction1, transaction3)
      }
    },
    ManualTest("newQuery().filterSeqDoesntContain()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1, tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = persistTransaction(day = 2, tags = Seq("tagA", "tagB"))
        val transaction3 = persistTransaction(day = 3, tags = Seq("tag"))

        val data = db
          .newQuery[Transaction]()
          .filterSeqDoesntContain(Keys.Transaction.tags, "tag")
          .data()

        data ==> Vector(transaction2)
      }
    },
    ManualTest("newQuery().sort()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(groupId = 1, day = 2)
        val transaction2 = persistTransaction(groupId = 1, day = 3)
        val transaction3 = persistTransaction(groupId = 2, day = 1)

        val data = db
          .newQuery[Transaction]()
          .sort(
            LokiJs.Sorting
              .descBy(Keys.Transaction.transactionGroupId)
              .thenAscBy(Keys.Transaction.createdDate))
          .data()

        data ==> Vector(transaction3, transaction1, transaction2)
      }
    },
    ManualTest("newQuery().limit()") {
      async {
        implicit val db = await(LocalDatabase.createInMemoryForTests())
        val transaction1 = persistTransaction(day = 1)
        val transaction2 = persistTransaction(day = 2)
        persistTransaction(day = 3)

        db.newQuery[Transaction]()
          .sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
          .limit(2)
          .data() ==> Seq(transaction1, transaction2)
      }
    },
    ManualTest("newQuery().findOne()") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(testTransactionWithId, transaction2, transaction3))

        db.newQuery[Transaction]().findOne(Keys.id, 99992L) ==> Some(transaction2)
      }
    },
    ManualTest("newQuery().count()") {
      async {
        val db = await(LocalDatabase.createInMemoryForTests())
        val transaction2 = testTransactionWithId.copy(idOption = Some(99992))
        val transaction3 = testTransactionWithId.copy(idOption = Some(99993))
        db.addAll(Seq(testTransactionWithId, transaction2, transaction3))

        db.newQuery[Transaction]().count() ==> 3
      }
    }
  )

  private def persistTransaction(
      groupId: Long = 1,
      day: Int = 1,
      category: Category = testCategory,
      description: String = "some description",
      detailDescription: String = "some detail description",
      tags: Seq[String] = Seq("some-tag"))(implicit db: LocalDatabase): Transaction = {
    val transaction = testTransactionWithIdA.copy(
      idOption = Some(EntityModification.generateRandomId()),
      transactionGroupId = groupId,
      categoryCode = category.code,
      description = description,
      detailDescription = detailDescription,
      tags = tags,
      createdDate = createDateTime(2012, JANUARY, day),
      transactionDate = createDateTime(2012, JANUARY, day),
      consumedDate = createDateTime(2012, JANUARY, day)
    )
    db.addAll(Seq(transaction))
    transaction
  }
}
