package tests

import common.testing.TestObjects._
import jsfacades.LokiJs
import jsfacades.LokiJs.{Filter, ResultSet}
import models.access.LocalDatabase
import models.access.DbQuery.Filter
import models.access.DbQueryImplicits._
import models.access.{DbQuery, DbResultSet, Fields, LocalDatabase}
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
private[tests] object LocalDatabaseResultSetTest extends ManualTestSuite {

  override def tests = Seq(
    // **************** Regular filter tests **************** //
    ManualTest("newQuery().filter(nullFilter)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Filter.NullFilter())
        .containsExactly(transaction1, transaction2, transaction3)
    },
    ManualTest("newQuery().filter(equal)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.id[Transaction] isEqualTo transaction2.id)
        .containsExactly(transaction2)
    },
    ManualTest("newQuery().filter(notEqual)") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.id[Transaction] isNotEqualTo transaction2.id)
        .containsExactly(transaction1, transaction3)
    },
    ManualTest("newQuery().filter(lessThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.createdDate < transaction3.createdDate)
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("newQuery().filter(greaterThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.createdDate > transaction1.createdDate)
        .containsExactly(transaction2, transaction3)
    },
    ManualTest("newQuery().filter(greaterOrEqualThan)") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.createdDate >= transaction2.createdDate)
        .containsExactly(transaction2, transaction3)
    },
    ManualTest("newQuery().filter(anyOf)") {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          Fields.Transaction.categoryCode isAnyOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("newQuery().filter(noneOf)") {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          Fields.Transaction.categoryCode isNoneOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction3)
    },
    ManualTest("newQuery().filter(containsIgnoreCase)") {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.description containsIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction1, transaction2)
    },
    ManualTest("newQuery().filter(doesntContainIgnoreCase)") {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.description doesntContainIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction3)
    },
    ManualTest("newQuery().filter(seqContains)") {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.tags contains "tag")
        .containsExactly(transaction1, transaction3)
    },
    ManualTest("newQuery().filter(seqDoesntContain)") {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Fields.Transaction.tags doesntContain "tag")
        .containsExactly(transaction2)
    },
    // **************** OR / AND filter tests **************** //
    ManualTest("newQuery().filter(or(equal, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()
      val transaction4 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3, transaction4)
        .assertFilteredWith({
          Fields.id[Transaction] isEqualTo transaction1.id
        } || {
          Fields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        })
        .containsExactly(transaction1, transaction2, transaction3)
    },
    ManualTest("newQuery().filter(and(equal, equal))") {
      val transaction1 = createTransaction(description = "abc", category = testCategoryA)
      val transaction2 = createTransaction(description = "abc", category = testCategoryB)
      val transaction3 = createTransaction(description = "def", category = testCategoryB)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          Fields.Transaction.description isEqualTo "abc"
        } && {
          Fields.Transaction.categoryCode isEqualTo testCategoryB.code
        })
        .containsExactly(transaction2)
    },
    ManualTest("newQuery().filter(and(anyOf, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          Fields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        } && {
          Fields.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id)
        })
        .containsExactly(transaction2)
    },
    ManualTest("newQuery().filter(or(and(anyOf, anyOf), and(anyOf, anyOf))") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          {
            (Fields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)) &&
            (Fields.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id))
          } || {
            (Fields.id[Transaction] isAnyOf Seq(transaction1.id, transaction3.id)) &&
            (Fields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id))
          }
        )
        .containsExactly(transaction2, transaction3)
    },
    // **************** Non-filter tests **************** //
    ManualTest("newQuery().sort()") {
      val transaction1 = createTransaction(groupId = 1, day = 2)
      val transaction2 = createTransaction(groupId = 1, day = 3)
      val transaction3 = createTransaction(groupId = 2, day = 1)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting
            .descBy(Fields.Transaction.transactionGroupId)
            .thenAscBy(Fields.Transaction.createdDate))
            .data())
        .containsExactlyInOrder(transaction3, transaction1, transaction2)
    },
    ManualTest("newQuery().limit()") {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting.ascBy(Fields.Transaction.createdDate))
            .limit(2)
            .data())
        .containsExactlyInOrder(transaction1, transaction2)
    },
    ManualTest("newQuery().findOne()") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(_.findOne(Keys.id, transaction2.id))
        .isEqualTo(Some(transaction2))
    },
    ManualTest("newQuery().count()") {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3).assertThat(_.count()).isEqualTo(3)
    }
  )

  private def withTransactions(transactions: Transaction*) = new Object {
    def assertFilteredWith(filter: Filter[Transaction]) = assertThat(_.filter(filter).data())

    def assertThat(resultSetFunc: DbResultSet[Transaction] => Any) = new Object {
      def containsExactly(expected: Transaction*): Future[Unit] = async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.addAll(transactions.toVector)
        resultSetFunc(db.newQuery[Transaction]()) match {
          case seq: Seq[_] => assertEqualIterables(seq.toSet, expected.toSet)
        }
      }

      def containsExactlyInOrder(expected: Transaction*): Future[Unit] = async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.addAll(transactions.toVector)
        resultSetFunc(db.newQuery[Transaction]()) match {
          case seq: Seq[_] => assertEqualIterables(seq, expected.toVector)
        }
      }

      def isEqualTo(expected: Any): Future[Unit] = async {
        val db = await(LocalDatabase.createInMemoryForTests())
        db.addAll(transactions.toVector)
        resultSetFunc(db.newQuery[Transaction]()) ==> expected
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
