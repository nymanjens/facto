package app.common.accounting

import app.common.testing.TestObjects.createTransaction
import app.common.testing.TestObjects.testAccountA
import app.common.testing.TestObjects.testAccountB
import app.common.testing.TestObjects.testCategory
import app.common.testing.TestObjects.testCategoryA
import app.common.testing.TestObjects.testCategoryB
import app.common.testing.TestObjects.testReservoirCashA
import app.common.testing.TestObjects.testReservoirHidden
import app.common.testing.TestObjects.testUserA
import app.common.testing.TestObjects.testUserB
import app.models.access.AppEntityAccess
import app.models.accounting.Transaction
import utest.TestSuite

import scala.collection.immutable.Seq

object ComplexQueryFilterTest extends TestSuite {

  override def tests = TestSuite {

    val testModule = new app.common.testing.TestModule

    implicit val fakeDatabase = testModule.fakeEntityAccess
    implicit val testAccountingConfig = testModule.testAccountingConfig
    fakeDatabase.addRemotelyAddedEntities(testUserA, testUserB)

    implicit val complexQueryFilter = new ComplexQueryFilter()

    "fromQuery()" - {
      "empty filter" - {
        val transaction1 = createTransaction(issuer = testUserA)
        val transaction2 = createTransaction(issuer = testUserB)
        val transaction3 = createTransaction(issuer = testUserB)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(s""" """)
          .containsExactly(transaction1, transaction2, transaction3)
      }
      "issuer filter" - {
        val transaction1 = createTransaction(issuer = testUserA)
        val transaction2 = createTransaction(issuer = testUserB)
        val transaction3 = createTransaction(issuer = testUserB)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(s""" user:"${testUserA.name}" """)
          .containsExactly(transaction1)
      }
      "beneficiary filter" - {
        val transaction1 = createTransaction(beneficiary = testAccountA)
        val transaction2 = createTransaction(beneficiary = testAccountB)
        val transaction3 = createTransaction(beneficiary = testAccountB)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(s""" b:"${testAccountA.longName}" """)
          .containsExactly(transaction1)
      }
      "reservoir filter" - {
        val transaction1 = createTransaction(reservoir = testReservoirCashA)
        val transaction2 = createTransaction(reservoir = testReservoirHidden)
        val transaction3 = createTransaction(reservoir = null)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(s""" r:"${testReservoirHidden.name}" """)
          .containsExactly(transaction2)
      }
      "category filter" - {
        val transaction1 = createTransaction(category = testCategoryA)
        val transaction2 = createTransaction(category = testCategoryA)
        val transaction3 = createTransaction(category = testCategoryB)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(s""" category:"${testCategory.name.substring(3)}" """)
          .containsExactly(transaction1, transaction2)
      }
      "description filter" - {
        val transaction1 = createTransaction(description = "cat dog fish")
        val transaction2 = createTransaction(description = "fish")
        val transaction3 = createTransaction(description = "cat")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("description:ish")
          .containsExactly(transaction1, transaction2)
      }
      "flow filter" - {
        val transaction1 = createTransaction(flow = 2.12)
        val transaction2 = createTransaction(flow = -2.12)
        val transaction3 = createTransaction(flow = -2.22)

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(" amount:-2.12")
          .containsExactly(transaction2)
      }
      "detail filter" - {
        val transaction1 = createTransaction(detailDescription = "this is a fish")
        val transaction2 = createTransaction(detailDescription = "fishes")
        val transaction3 = createTransaction(detailDescription = "this is a cat")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(" detail:a")
          .containsExactly(transaction1, transaction3)
      }
      "tag filter" - {
        val transaction1 = createTransaction(tags = Seq("cat", "dog"))
        val transaction2 = createTransaction(tags = Seq("fish"))
        val transaction3 = createTransaction(tags = Seq())

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(" t:cat")
          .containsExactly(transaction1)
      }

      "filter without prefix" - {
        val transaction1 = createTransaction(description = "cat dog fish")
        val transaction2 = createTransaction(description = "fish")
        val transaction3 = createTransaction(description = "cat")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("  fish  ")
          .containsExactly(transaction1, transaction2)
      }
      "filter with negation" - {
        val transaction1 = createTransaction(description = "cat dog fish")
        val transaction2 = createTransaction(description = "fish")
        val transaction3 = createTransaction(description = "cat")

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("-description:fish")
          .containsExactly(transaction3)
        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery(""" -"dog f" """)
          .containsExactly(transaction2, transaction3)
      }
      "filter with multiple parts" - {
        val transaction1 = createTransaction(description = "cat dog fish", tags = Seq("monkey"))
        val transaction2 = createTransaction(description = "fish")
        val transaction3 = createTransaction(description = "cat", tags = Seq("monkey"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("fish tag:monkey")
          .containsExactly(transaction1)
        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("fish -tag:monkey")
          .containsExactly(transaction2)
      }
    }

    "parsePrefixAndSuffix()" - {
      "single colon" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          complexQueryFilter.parsePrefixAndSuffix(s"$prefixString:some value") ==>
            Some((prefix, "some value"))
        }
      }
      "multiple colons" - {
        for {
          prefix <- Prefix.all
          prefixString <- prefix.prefixStrings
        } {
          complexQueryFilter
            .parsePrefixAndSuffix(s"$prefixString:some: value") ==> Some((prefix, "some: value"))
        }
      }
      "wrong prefix" - {
        complexQueryFilter.parsePrefixAndSuffix(s"unknownPrefix:some: value") ==> None
      }
      "empty string" - {
        complexQueryFilter.parsePrefixAndSuffix(s"") ==> None
      }
      "empty prefix" - {
        complexQueryFilter.parsePrefixAndSuffix(s":some value") ==> None
      }
      "empty suffix" - {
        complexQueryFilter.parsePrefixAndSuffix(s"category:") ==> None
      }
      "no colons" - {
        complexQueryFilter.parsePrefixAndSuffix(s"category") ==> None
      }
    }

    "splitInParts()" - {
      "empty string" - {
        complexQueryFilter.splitInParts("") ==> Seq()
      }
      "negation" - {
        complexQueryFilter.splitInParts("-a c -def") ==>
          Seq(QueryPart.not("a"), QueryPart("c"), QueryPart.not("def"))
      }
      "double negation" - {
        complexQueryFilter.splitInParts("--a") ==> Seq(QueryPart.not("-a"))
      }
      "double quotes" - {
        complexQueryFilter.splitInParts(""" "-a c" """) ==> Seq(QueryPart("-a c"))
      }
      "single quotes" - {
        complexQueryFilter.splitInParts(" '-a c' ") ==> Seq(QueryPart("-a c"))
      }
      "negated quotes" - {
        complexQueryFilter.splitInParts("-'XX YY'") ==> Seq(QueryPart.not("XX YY"))
      }
      "quote after colon" - {
        complexQueryFilter.splitInParts("-don:'t wont'") ==> Seq(QueryPart.not("don:t wont"))
      }
      "quote inside text" - {
        complexQueryFilter.splitInParts("-don't won't") ==> Seq(QueryPart.not("don't"), QueryPart("won't"))
      }
    }
  }

  private def withTransactions(transactions: Transaction*)(implicit
      complexQueryFilter: ComplexQueryFilter,
      entityAccess: AppEntityAccess,
  ) =
    new Object {
      def assertThatQuery(query: String) = new Object {
        def containsExactly(expected: Transaction*): Unit = {
          val result = transactions.filter(complexQueryFilter.fromQuery(query).apply)
          assertEqualIterables(result.toSet, expected.toSet)
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
        assertProperty(_.issuer.name)
        assertProperty(_.beneficiary.longName)
        assertProperty(_.id)
        iterable1 ==> iterable2
      }
    }
}
