package jsfacades

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import jsfacades.LokiJs.ResultSet
import models.accounting.Transaction
import models.accounting.config.Category
import models.manager.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala2js.Converters._
import scala2js.ConvertersTest._
import scala2js.Keys

object LokiResultSetFakeTest extends TestSuite {

  override def tests = TestSuite {
    "LokiJs.ResultSet.Fake" - {
      "newQuery().filter()" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filter(Keys.id, transaction2.id).data())
          .containsExactly(transaction2)
      }
      "newQuery().filterNot()" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterNot(Keys.id, transaction2.id).data())
          .containsExactly(transaction1, transaction3)
      }
      "newQuery().filterLessThan()" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterLessThan(Keys.Transaction.createdDate, transaction3.createdDate).data())
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filterGreaterThan()" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterGreaterThan(Keys.Transaction.createdDate, transaction1.createdDate)
            .data())
          .containsExactly(transaction2, transaction3)
      }
      "newQuery().filterAnyOf()" - {
        val transaction1 = createTransaction(category = testCategoryA)
        val transaction2 = createTransaction(category = testCategoryB)
        val transaction3 = createTransaction(category = testCategoryC)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(
            _.filterAnyOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code))
              .data())
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filterNoneOf()" - {
        val transaction1 = createTransaction(category = testCategoryA)
        val transaction2 = createTransaction(category = testCategoryB)
        val transaction3 = createTransaction(category = testCategoryC)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(
            _.filterNoneOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code))
              .data())
          .containsExactly(transaction3)
      }
      "newQuery().filterContainsIgnoreCase()" - {
        val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        val transaction2 = createTransaction(description = "BBBB.*cccc")
        val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterContainsIgnoreCase(Keys.Transaction.description, "BBBB.*cccc")
            .data())
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filterDoesntContainIgnoreCase()" - {
        val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        val transaction2 = createTransaction(description = "BBBB.*cccc")
        val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterDoesntContainIgnoreCase(Keys.Transaction.description, "BBBB.*cccc")
            .data())
          .containsExactly(transaction3)
      }
      "newQuery().filterSeqContains()" - {
        val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
        val transaction3 = createTransaction(tags = Seq("tag"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterSeqContains(Keys.Transaction.tags, "tag")
            .data())
          .containsExactly(transaction1, transaction3)
      }
      "newQuery().filterSeqDoesntContain()" - {
        val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
        val transaction3 = createTransaction(tags = Seq("tag"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.filterSeqDoesntContain(Keys.Transaction.tags, "tag")
            .data())
          .containsExactly(transaction2)
      }
      "newQuery().sort()" - {
        val transaction1 = createTransaction(groupId = 1, day = 2)
        val transaction2 = createTransaction(groupId = 1, day = 3)
        val transaction3 = createTransaction(groupId = 2, day = 1)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(
            _.sort(LokiJs.Sorting
              .descBy(Keys.Transaction.transactionGroupId)
              .thenAscBy(Keys.Transaction.createdDate))
              .data())
          .containsExactlyInOrder(transaction3, transaction1, transaction2)
      }
      "newQuery().limit()" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(
            _.sort(LokiJs.Sorting.ascBy(Keys.Transaction.createdDate))
              .limit(2)
              .data())
          .containsExactlyInOrder(transaction1, transaction2)
      }
      "newQuery().findOne()" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertThat(_.findOne(Keys.id, transaction2.id))
          .isEqualTo(Some(transaction2))
      }
      "newQuery().count()" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3).assertThat(_.count()).isEqualTo(3)
      }
    }
  }

  private def createTransaction(groupId: Long = 1273984,
                                day: Int = 25,
                                category: Category = testCategory,
                                description: String = "some description",
                                detailDescription: String = "some detail description",
                                tags: Seq[String] = Seq("some-tag")): Transaction = {
    testTransactionWithId.copy(
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
  }
  private def withTransactions(transactions: Transaction*) = new Object {
    def assertThat(resultSetFunc: ResultSet[Transaction] => Any) = new Object {
      def containsExactly(expected: Transaction*): Unit = {
        resultSetFunc(LokiJs.ResultSet.fake(transactions.toVector)) match {
          case seq: Seq[_] => assertEqualIterables(seq.toSet, expected.toSet)
        }
      }

      def containsExactlyInOrder(expected: Transaction*): Unit = {
        resultSetFunc(LokiJs.ResultSet.fake(transactions.toVector)) match {
          case seq: Seq[_] => assertEqualIterables(seq, expected.toVector)
        }
      }

      def isEqualTo(expected: Any): Unit = {
        resultSetFunc(LokiJs.ResultSet.fake(transactions.toVector)) ==> expected
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
