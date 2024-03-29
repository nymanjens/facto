package app.common.accounting

import app.common.testing.TestObjects.testCategory
import app.common.testing.TestObjects._
import app.models.access.AppEntityAccess
import app.models.accounting.Transaction
import hydro.models.access.EntityAccess
import utest._
import app.common.accounting.ComplexQueryFilter.Prefix
import app.common.accounting.ComplexQueryFilter.QueryPart

import scala.collection.immutable.Seq
import scala.language.reflectiveCalls

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
          .containsExactly(transaction1, transaction2)
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
          .assertThatQuery("fish (tag:monkey sh)")
          .containsExactly(transaction1)
        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("fish -(tag:monkey sh)")
          .containsExactly(transaction2)
      }
      "filter with OR" - {
        val transaction1 = createTransaction(description = "cat dog fish", tags = Seq("monkey"))
        val transaction2 = createTransaction(description = "fish")
        val transaction3 = createTransaction(description = "donkey", tags = Seq("monkey"))

        withTransactions(transaction1, transaction2, transaction3)
          .assertThatQuery("(fish -dog) OR (donkey tag:monkey)")
          .containsExactly(transaction2, transaction3)
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
      def literal(s: String): QueryPart = QueryPart.Literal(s)
      def not(q: QueryPart): QueryPart = QueryPart.Not(q)
      def and(qs: QueryPart*): QueryPart = QueryPart.And(Seq(qs: _*))
      def or(qs: QueryPart*): QueryPart = QueryPart.Or(Seq(qs: _*))

      "empty string" - {
        complexQueryFilter.splitInParts("") ==> Seq()
      }
      "negation" - {
        complexQueryFilter.splitInParts("-a c -def") ==>
          Seq(not(literal("a")), literal("c"), not(literal("def")))
      }
      "double negation" - {
        complexQueryFilter.splitInParts("--a") ==> Seq(not(literal("-a")))
      }
      "double quotes" - {
        complexQueryFilter.splitInParts(""" "-a c" """) ==> Seq(literal("-a c"))
      }
      "double quotes without closing quotes" - {
        complexQueryFilter.splitInParts(""" "a b """) ==> Seq(literal("a b"))
      }
      "single quotes" - {
        complexQueryFilter.splitInParts(" '-a c' ") ==> Seq(literal("-a c"))
      }
      "negated quotes" - {
        complexQueryFilter.splitInParts("-'XX YY'") ==> Seq(not(literal("XX YY")))
      }
      "quote after colon" - {
        complexQueryFilter.splitInParts("-don:'t wont'") ==> Seq(not(literal("don:t wont")))
      }
      "quote inside text" - {
        complexQueryFilter.splitInParts("-don't won't") ==> Seq(
          not(literal("don't")),
          literal("won't"),
        )
      }
      "with simple brackets" - {
        complexQueryFilter.splitInParts("a (b c)") ==> Seq(
          literal("a"),
          and(literal("b"), literal("c")),
        )
      }
      "with simple brackets and negation" - {
        complexQueryFilter.splitInParts("a -(b c)") ==> Seq(
          literal("a"),
          not(and(literal("b"), literal("c"))),
        )
      }
      "brackets as part of text" - {
        complexQueryFilter.splitInParts("func()") ==> Seq(
          literal("func()")
        )
      }
      "brackets and quotes" - {
        complexQueryFilter.splitInParts(""" ("abc ( " def) """) ==> Seq(
          and(literal("abc ("), literal("def"))
        )
      }
      "nested brackets" - {
        complexQueryFilter.splitInParts("a -((b c) d)") ==> Seq(
          literal("a"),
          not(
            and(and(literal("b"), literal("c")), literal("d"))
          ),
        )
      }
      "nested brackets with inner one as part of text" - {
        complexQueryFilter.splitInParts("a ( func() ) b") ==> Seq(
          literal("a"),
          literal("func()"),
          literal("b"),
        )
      }
      "OR statement: Simple" - {
        complexQueryFilter.splitInParts("a OR b") ==> Seq(
          or(literal("a"), literal("b"))
        )
      }
      "OR statement: Multiple with brackets" - {
        complexQueryFilter.splitInParts("a b OR (c OR d or e) OR f g") ==> Seq(
          literal("a"),
          or(
            or(
              literal("b"),
              or(
                or(
                  literal("c"),
                  literal("d"),
                ),
                literal("e"),
              ),
            ),
            literal("f"),
          ),
          literal("g"),
        )
      }
      "OR and AND combined" - {
        complexQueryFilter.splitInParts("(a b) OR (c d)") ==> Seq(
          or(
            and(
              literal("a"),
              literal("b"),
            ),
            and(
              literal("c"),
              literal("d"),
            ),
          )
        )
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
