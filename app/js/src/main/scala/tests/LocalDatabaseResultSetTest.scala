package tests

import common.testing.TestObjects._
import models.access.DbQuery.Filter
import models.access.DbQueryImplicits._
import models.access._
import models.access.webworker.LocalDatabaseWebWorkerApi
import models.accounting.Transaction
import tests.ManualTests.{ManualTest, ManualTestSuite}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

// Note that this is a manual test because the Rhino javascript engine used for tests
// is incompatible with Loki.
private[tests] class LocalDatabaseResultSetTest extends ManualTestSuite {

  implicit private val webWorker: LocalDatabaseWebWorkerApi =
    new models.access.webworker.Module().localDatabaseWebWorkerApiStub

  override def tests = Seq(
    // **************** Regular filter tests **************** //
    ManualTest("queryExecutor().filter(nullFilter)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Filter.NullFilter())
        .containsExactly(transaction1, transaction2, transaction3)
    },
    ManualTest("queryExecutor().filter(equal)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.id[Transaction] === transaction2.id)
        .containsExactly(transaction2)
    },
    ManualTest("queryExecutor().filter(notEqual)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.id[Transaction] !== transaction2.id)
        .containsExactly(transaction1, transaction3)
    },
    ManualTest("queryExecutor().filter(lessThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.createdDate < transaction3.createdDate)
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("queryExecutor().filter(greaterThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.createdDate > transaction1.createdDate)
        .containsExactly(transaction2, transaction3)
    },
    ManualTest("queryExecutor().filter(greaterOrEqualThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.createdDate >= transaction2.createdDate)
        .containsExactly(transaction2, transaction3)
    },
    ManualTest("queryExecutor().filter(anyOf)") {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          ModelField.Transaction.categoryCode isAnyOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("queryExecutor().filter(noneOf)") {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          ModelField.Transaction.categoryCode isNoneOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction3)
    },
    ManualTest("queryExecutor().filter(containsIgnoreCase)") {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.description containsIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("queryExecutor().filter(doesntContainIgnoreCase)") {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.description doesntContainIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction3)
    },
    ManualTest("queryExecutor().filter(seqContains)") {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.tags contains "tag")
        .containsExactly(transaction1, transaction3)
    },
    ManualTest("queryExecutor().filter(seqDoesntContain)") {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelField.Transaction.tags doesntContain "tag")
        .containsExactly(transaction2)
    },
    // **************** OR / AND filter tests **************** //
    ManualTest("queryExecutor().filter(or(equal, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()
      val transaction4 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3, transaction4)
        .assertFilteredWith({
          ModelField.id[Transaction] === transaction1.id
        } || {
          ModelField.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        })
        .containsExactly(transaction1, transaction2, transaction3)
    },
    ManualTest("queryExecutor().filter(and(equal, equal))") {
      val transaction1 = createTransaction(description = "abc", category = testCategoryA)
      val transaction2 = createTransaction(description = "abc", category = testCategoryB)
      val transaction3 = createTransaction(description = "def", category = testCategoryB)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          ModelField.Transaction.description === "abc"
        } && {
          ModelField.Transaction.categoryCode === testCategoryB.code
        })
        .containsExactly(transaction2)
    },
    ManualTest("queryExecutor().filter(and(anyOf, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          ModelField.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        } && {
          ModelField.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id)
        })
        .containsExactly(transaction2)
    },
    ManualTest("queryExecutor().filter(or(and(anyOf, anyOf), and(anyOf, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          {
            (ModelField.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)) &&
            (ModelField.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id))
          } || {
            (ModelField.id[Transaction] isAnyOf Seq(transaction1.id, transaction3.id)) &&
            (ModelField.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id))
          }
        )
        .containsExactly(transaction2, transaction3)
    },
    // **************** Non-filter tests **************** //
    ManualTest("queryExecutor().sort()") {
      val transaction1 = createTransaction(groupId = 1, day = 2)
      val transaction2 = createTransaction(groupId = 1, day = 3)
      val transaction3 = createTransaction(groupId = 2, day = 1)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting
            .descBy(ModelField.Transaction.transactionGroupId)
            .thenAscBy(ModelField.Transaction.createdDate))
            .data())
        .containsExactlyInOrder(transaction3, transaction1, transaction2)
    },
    ManualTest("queryExecutor().limit()") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting.ascBy(ModelField.Transaction.createdDate))
            .limit(2)
            .data())
        .containsExactlyInOrder(transaction1, transaction2)
    },
    ManualTest("queryExecutor().findOne()") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(_.findOne(ModelField.id, transaction2.id))
        .isEqualTo(Some(transaction2))
    },
    ManualTest("queryExecutor().count()") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3).assertThat(_.count()).isEqualTo(3)
    }
  )

  private def withTransactions(transactions: Transaction*) = new Object {
    def assertFilteredWith(filter: Filter[Transaction]) = assertThat(_.filter(filter).data())

    def assertThat(resultSetFunc: DbResultSet.Async[Transaction] => Future[Any]) = new Object {
      def containsExactly(expected: Transaction*): Future[Unit] = async {
        val db = await(LocalDatabaseImpl.createInMemoryForTests())
        await(db.resetAndInitialize())
        await(db.addAll(transactions.toVector))
        await(resultSetFunc(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()))) match {
          case seq: Seq[_] => assertEqualIterables(seq.toSet, expected.toSet)
        }
      }

      def containsExactlyInOrder(expected: Transaction*): Future[Unit] = async {
        val db = await(LocalDatabaseImpl.createInMemoryForTests())
        await(db.resetAndInitialize())
        await(db.addAll(transactions.toVector))
        await(resultSetFunc(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()))) match {
          case seq: Seq[_] => assertEqualIterables(seq, expected.toVector)
        }
      }

      def isEqualTo(expected: Any): Future[Unit] = async {
        val db = await(LocalDatabaseImpl.createInMemoryForTests())
        await(db.resetAndInitialize())
        await(db.addAll(transactions.toVector))
        await(resultSetFunc(DbResultSet.fromExecutor(db.queryExecutor[Transaction]()))) ==> expected
      }
    }

    private def assertEqualIterables(iterable1: Iterable[_], iterable2: Iterable[Transaction]): Unit = {
      def assertProperty(propertyFunc: Transaction => Any): Unit = {
        iterable1.map(_.asInstanceOf[Transaction]).map(propertyFunc) ==> iterable2.map(propertyFunc)
      }
      assertProperty(_.description)
      assertProperty(_.detailDescription)
      assertProperty(_.categoryCode)
      assertProperty(_.tags.mkString(","))
      assertProperty(_.createdDate)
      assertProperty(_.transactionGroupId)
      assertProperty(_.id)
      iterable1 ==> iterable2
    }
  }
}
