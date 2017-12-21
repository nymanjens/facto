package jsfacades

import java.time.Month.JANUARY

import common.testing.TestObjects._
import common.time.LocalDateTimes.createDateTime
import jsfacades.LokiJs.{Filter, ResultSet}
import models.access.Fields
import models.accounting.Transaction
import models.accounting.config.Category
import models.modification.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls
import scala2js.Converters._
import scala2js.Keys

object LokiResultSetFakeTest extends TestSuite {

  override def tests = TestSuite {
    "LokiJs.ResultSet.Fake" - {
      // **************** Regular filter tests **************** //
      "newQuery().filter(nullFilter)" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.nullFilter)
          .containsExactly(transaction1, transaction2, transaction3)
      }
      "newQuery().filter(equal)" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.equal(Keys.id, transaction2.id))
          .containsExactly(transaction2)
      }
      "newQuery().filter(notEqual)" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.notEqual(Keys.id, transaction2.id))
          .containsExactly(transaction1, transaction3)
      }
      "newQuery().filter(lessThan)" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.lessThan(Keys.Transaction.createdDate, transaction3.createdDate))
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filter(greaterThan)" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.greaterThan(Keys.Transaction.createdDate, transaction1.createdDate))
          .containsExactly(transaction2, transaction3)
      }
      "newQuery().filter(greaterOrEqualThan)" - {
        val transaction1 = createTransaction(day = 1)
        val transaction2 = createTransaction(day = 2)
        val transaction3 = createTransaction(day = 3)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(
            Filter.greaterOrEqualThan(Keys.Transaction.createdDate, transaction2.createdDate))
          .containsExactly(transaction2, transaction3)
      }
      "newQuery().filter(anyOf)" - {
        val transaction1 = createTransaction(category = testCategoryA)
        val transaction2 = createTransaction(category = testCategoryB)
        val transaction3 = createTransaction(category = testCategoryC)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(
            Filter.anyOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code)))
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filter(noneOf)" - {
        val transaction1 = createTransaction(category = testCategoryA)
        val transaction2 = createTransaction(category = testCategoryB)
        val transaction3 = createTransaction(category = testCategoryC)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(
            Filter.noneOf(Keys.Transaction.categoryCode, Seq(testCategoryA.code, testCategoryB.code)))
          .containsExactly(transaction3)
      }
      "newQuery().filter(containsIgnoreCase)" - {
        val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        val transaction2 = createTransaction(description = "BBBB.*cccc")
        val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.containsIgnoreCase(Keys.Transaction.description, "BBBB.*cccc"))
          .containsExactly(transaction1, transaction2)
      }
      "newQuery().filter(doesntContainIgnoreCase)" - {
        val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
        val transaction2 = createTransaction(description = "BBBB.*cccc")
        val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.doesntContainIgnoreCase(Keys.Transaction.description, "BBBB.*cccc"))
          .containsExactly(transaction3)
      }
      "newQuery().filter(seqContains)" - {
        val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
        val transaction3 = createTransaction(tags = Seq("tag"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.seqContains(Keys.Transaction.tags, "tag"))
          .containsExactly(transaction1, transaction3)
      }
      "newQuery().filter(seqDoesntContain)" - {
        val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
        val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
        val transaction3 = createTransaction(tags = Seq("tag"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.seqDoesntContain(Keys.Transaction.tags, "tag"))
          .containsExactly(transaction2)
      }

      // **************** OR / AND filter tests **************** //
      "newQuery().filter(or(equal, anyOf))" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()
        val transaction4 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3, transaction4)
          .assertFilteredWith(
            Filter.or(
              Filter.equal(Keys.id, transaction1.id),
              Filter.anyOf(Keys.id, Seq(transaction2.id, transaction3.id))))
          .containsExactly(transaction1, transaction2, transaction3)
      }
      "newQuery().filter(and(equal, equal))" - {
        val transaction1 = createTransaction(description = "abc", category = testCategoryA)
        val transaction2 = createTransaction(description = "abc", category = testCategoryB)
        val transaction3 = createTransaction(description = "def", category = testCategoryB)

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(
            Filter.and(
              Filter.equal(Keys.Transaction.description, "abc"),
              Filter.equal(Keys.Transaction.categoryCode, testCategoryB.code)))
          .containsExactly(transaction2)
      }
      "newQuery().filter(and(anyOf, anyOf))" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(
            Filter.and(
              Filter.anyOf(Keys.id, Seq(transaction2.id, transaction3.id)),
              Filter.anyOf(Keys.id, Seq(transaction1.id, transaction2.id))))
          .containsExactly(transaction2)
      }
      "newQuery().filter(or(and(anyOf, anyOf), and(anyOf, anyOf))" - {
        val transaction1 = createTransaction()
        val transaction2 = createTransaction()
        val transaction3 = createTransaction()

        withTransactions(transaction1, transaction2, transaction3)
          .assertFilteredWith(Filter.or(
            Filter.and(
              Filter.anyOf(Keys.id, Seq(transaction2.id, transaction3.id)),
              Filter.anyOf(Keys.id, Seq(transaction1.id, transaction2.id))),
            Filter.and(
              Filter.anyOf(Keys.id, Seq(transaction1.id, transaction3.id)),
              Filter.anyOf(Keys.id, Seq(transaction2.id, transaction3.id)))
          ))
          .containsExactly(transaction2, transaction3)
      }

      // **************** Non-filter tests **************** //
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
          .assertThat(_.findOne(Fields.id, transaction2.id))
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
    def assertFilteredWith(filter: Filter[Transaction]) = assertThat(_.filter(filter).data())

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
