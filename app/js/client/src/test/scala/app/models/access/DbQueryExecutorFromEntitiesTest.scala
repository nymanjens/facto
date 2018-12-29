package app.models.access

import java.time.Month.JANUARY

import app.common.testing.TestObjects._
import hydro.common.time.LocalDateTimes.createDateTime
import app.models.access.DbQuery.Filter


import app.models.access.DbQueryImplicits._


import app.models.accounting.Transaction
import app.models.accounting.config.Category
import app.models.modification.EntityModification
import utest._

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls

object DbQueryExecutorFromEntitiesTest extends TestSuite {

  override def tests = TestSuite {
    // **************** Regular filter tests **************** //
    "filter(nullFilter)" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(Filter.NullFilter())
        .containsExactly(transaction1, transaction2, transaction3)
    }
    "filter(equal)" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.id[Transaction] === transaction2.id)
        .containsExactly(transaction2)
    }
    "filter(notEqual)" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.id[Transaction] !== transaction2.id)
        .containsExactly(transaction1, transaction3)
    }
    "filter(lessThan)" - {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.createdDate < transaction3.createdDate)
        .containsExactly(transaction1, transaction2)
    }
    "filter(greaterThan)" - {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.createdDate > transaction1.createdDate)
        .containsExactly(transaction2, transaction3)
    }
    "filter(greaterOrEqualThan)" - {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.createdDate >= transaction2.createdDate)
        .containsExactly(transaction2, transaction3)
    }
    "filter(anyOf)" - {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          ModelFields.Transaction.categoryCode isAnyOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction1, transaction2)
    }
    "filter(noneOf)" - {
      val transaction1 = createTransaction(category = testCategoryA)
      val transaction2 = createTransaction(category = testCategoryB)
      val transaction3 = createTransaction(category = testCategoryC)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          ModelFields.Transaction.categoryCode isNoneOf Seq(testCategoryA.code, testCategoryB.code))
        .containsExactly(transaction3)
    }
    "filter(containsIgnoreCase)" - {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.description containsIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction1, transaction2)
    }
    "filter(doesntContainIgnoreCase)" - {
      val transaction1 = createTransaction(description = "prefix\nAAAA_bbbb.*CCCC_dddd\nsuffix")
      val transaction2 = createTransaction(description = "BBBB.*cccc")
      val transaction3 = createTransaction(description = "prefix\nBBBBcccc\nsuffix")

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.description doesntContainIgnoreCase "BBBB.*cccc")
        .containsExactly(transaction3)
    }
    "filter(seqContains)" - {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.tags contains "tag")
        .containsExactly(transaction1, transaction3)
    }
    "filter(seqDoesntContain)" - {
      val transaction1 = createTransaction(tags = Seq("tagA", "tagB", "tag"))
      val transaction2 = createTransaction(tags = Seq("tagA", "tagB"))
      val transaction3 = createTransaction(tags = Seq("tag"))

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(ModelFields.Transaction.tags doesntContain "tag")
        .containsExactly(transaction2)
    }
    // **************** OR / AND filter tests **************** //
    "filter(or(equal, anyOf))" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()
      val transaction4 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3, transaction4)
        .assertFilteredWith({
          ModelFields.id[Transaction] === transaction1.id
        } || {
          ModelFields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        })
        .containsExactly(transaction1, transaction2, transaction3)
    }
    "filter(and(equal, equal))" - {
      val transaction1 = createTransaction(description = "abc", category = testCategoryA)
      val transaction2 = createTransaction(description = "abc", category = testCategoryB)
      val transaction3 = createTransaction(description = "def", category = testCategoryB)

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          ModelFields.Transaction.description === "abc"
        } && {
          ModelFields.Transaction.categoryCode === testCategoryB.code
        })
        .containsExactly(transaction2)
    }
    "filter(and(anyOf, anyOf))" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith({
          ModelFields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)
        } && {
          ModelFields.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id)
        })
        .containsExactly(transaction2)
    }
    "filter(or(and(anyOf, anyOf), and(anyOf, anyOf))" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertFilteredWith(
          {
            (ModelFields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id)) &&
            (ModelFields.id[Transaction] isAnyOf Seq(transaction1.id, transaction2.id))
          } || {
            (ModelFields.id[Transaction] isAnyOf Seq(transaction1.id, transaction3.id)) &&
            (ModelFields.id[Transaction] isAnyOf Seq(transaction2.id, transaction3.id))
          }
        )
        .containsExactly(transaction2, transaction3)
    }
    // **************** Non-filter tests **************** //
    "sort()" - {
      val transaction1 = createTransaction(groupId = 1, day = 2)
      val transaction2 = createTransaction(groupId = 1, day = 3)
      val transaction3 = createTransaction(groupId = 2, day = 1)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting
            .descBy(ModelFields.Transaction.transactionGroupId)
            .thenAscBy(ModelFields.Transaction.createdDate))
            .data())
        .containsExactlyInOrder(transaction3, transaction1, transaction2)
    }
    "limit()" - {
      val transaction1 = createTransaction(day = 1)
      val transaction2 = createTransaction(day = 2)
      val transaction3 = createTransaction(day = 3)

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(
          _.sort(DbQuery.Sorting.ascBy(ModelFields.Transaction.createdDate))
            .limit(2)
            .data())
        .containsExactlyInOrder(transaction1, transaction2)
    }
    "findOne()" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3)
        .assertThat(_.findOne(ModelFields.id[Transaction] === transaction2.id))
        .isEqualTo(Some(transaction2))
    }
    "count()" - {
      val transaction1 = createTransaction()
      val transaction2 = createTransaction()
      val transaction3 = createTransaction()

      withTransactions(transaction1, transaction2, transaction3).assertThat(_.count()).isEqualTo(3)
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

    def assertThat(resultSetFunc: DbResultSet.Sync[Transaction] => Any) = new Object {
      def containsExactly(expected: Transaction*): Unit = {
        resultSetFunc(dbResultSet) match {
          case seq: Seq[_] => assertEqualIterables(seq.toSet, expected.toSet)
        }
      }

      def containsExactlyInOrder(expected: Transaction*): Unit = {
        resultSetFunc(dbResultSet) match {
          case seq: Seq[_] => assertEqualIterables(seq, expected.toVector)
        }
      }

      def isEqualTo(expected: Any): Unit = {
        resultSetFunc(dbResultSet) ==> expected
      }
    }

    private def dbResultSet: DbResultSet.Sync[Transaction] =
      DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(transactions.toVector))

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
