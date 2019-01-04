package hydro.common

import org.specs2.matcher.MatchResult
import org.specs2.mutable._

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.Random

class OrderTokenTest extends Specification {

  private val random = new Random

  "equality" in {
    OrderToken(List(1, 2)) mustEqual OrderToken(List(1, 2))
    OrderToken(List(1, 2)) mustNotEqual OrderToken(List(1, 2, 1))
  }

  "compareTo" in {
    def testCompare(lesser: OrderToken, greater: OrderToken): MatchResult[OrderToken] = {
      lesser must beLessThan(greater)
      greater must beGreaterThan(lesser)
    }
    testCompare(lesser = OrderToken(List(1)), greater = OrderToken(List(2)))
    testCompare(lesser = OrderToken(List(1, 2, -100)), greater = OrderToken(List(1, 2)))
    testCompare(lesser = OrderToken(List(1, 2)), greater = OrderToken(List(1, 2, 100)))
    testCompare(lesser = OrderToken(List(1, 2, Int.MinValue)), greater = OrderToken(List(1, 2, Int.MaxValue)))
  }

  "middleBetween" in {
    "manual test cases" in {
      "tralilng zeros are removed" in {
        OrderToken.middleBetween(None, None) mustEqual OrderToken(List(0))
        OrderToken.middleBetween(someToken(-10), someToken(10)) mustEqual OrderToken(List(0))
        OrderToken.middleBetween(someToken(99, -10, 99), someToken(99, 10)) mustEqual OrderToken(List(99))
      }

      "one difference without carry-over" in {
        OrderToken.middleBetween(
          someToken(10, 20, Int.MaxValue, Int.MaxValue, Int.MaxValue - 4),
          someToken(10, 21, Int.MinValue, Int.MinValue, Int.MinValue + 3)) mustEqual
          OrderToken(List(10, 20, Int.MaxValue, Int.MaxValue, Int.MaxValue))
        OrderToken.middleBetween(
          someToken(10, 20, Int.MaxValue, Int.MaxValue, Int.MaxValue - 2),
          someToken(10, 21, 0, 0, 4)) mustEqual OrderToken(List(10, 21, Int.MinValue / 2 - 1))
      }
      "one difference with carry-over" in {
        OrderToken.middleBetween(
          someToken(10, 20, Int.MaxValue, Int.MaxValue, Int.MaxValue - 3, 123),
          someToken(10, 21, Int.MinValue, Int.MinValue, Int.MinValue + 4)) mustEqual
          OrderToken(List(10, 21, Int.MinValue, Int.MinValue, Int.MinValue))
        OrderToken.middleBetween(
          someToken(10, 20, Int.MaxValue, Int.MaxValue, Int.MaxValue - 2, 123),
          someToken(10, 21, Int.MinValue, Int.MinValue, Int.MinValue + 5, 456)) mustEqual
          OrderToken(List(10, 21, Int.MinValue, Int.MinValue, Int.MinValue + 1))
      }
    }
    "always append is sorted" in {
      def alwaysAppendTokenStream(nextValue: OrderToken = OrderToken.middle): Stream[OrderToken] = {
        nextValue #:: alwaysAppendTokenStream(nextValue = OrderToken.middleBetween(Some(nextValue), None))
      }
      alwaysAppendTokenStream().take(500).toVector must beSorted
    }
    "always prepend is sorted in reverse" in {
      def alwaysPrependTokenStream(nextValue: OrderToken = OrderToken.middle): Stream[OrderToken] = {
        nextValue #:: alwaysPrependTokenStream(nextValue = OrderToken.middleBetween(None, Some(nextValue)))
      }
      alwaysPrependTokenStream().take(500).toVector.reverse must beSorted
    }
    "random walk always returns token between given tokens" in {
      val allPreviousTokens = mutable.SortedSet(OrderToken(List(Int.MaxValue)))
      for (i <- 1 to 9000) yield {
        val lastTokens = allPreviousTokens.toSeq.drop(allPreviousTokens.size - 40)
        val tokenLeftIndex = random.nextInt(lastTokens.size)
        val tokenLeft = lastTokens(tokenLeftIndex)
        val maybeTokenRight =
          if (tokenLeftIndex + 1 < lastTokens.size) Some(lastTokens(tokenLeftIndex + 1)) else None

        val calculatedToken = OrderToken.middleBetween(Some(tokenLeft), maybeTokenRight)
        allPreviousTokens += calculatedToken

        // assertions
        maybeTokenRight match {
          case Some(right) if tokenLeft == right => calculatedToken mustEqual tokenLeft
          case Some(right) =>
            calculatedToken must beGreaterThan(tokenLeft)
            calculatedToken must beLessThan(right)
          case None =>
            calculatedToken must beGreaterThan(tokenLeft)
        }

        1 mustEqual 1 // dummy check to satisfy `in{}`'s return value
      }
    }
  }
  "evenlyDistributedValuesBetween" in {
    "With None bounds" in {
      OrderToken.evenlyDistributedValuesBetween(1, None, None) mustEqual Seq(OrderToken(List(0)))
      OrderToken.evenlyDistributedValuesBetween(3, None, None) mustEqual
        Seq(OrderToken(List(Int.MinValue / 2)), OrderToken(List(0)), OrderToken(List(Int.MaxValue / 2)))
    }
    "With single None bound" in {
      OrderToken.evenlyDistributedValuesBetween(4, None, someToken(0)) mustEqual
        Seq(
          OrderToken(List(Int.MinValue - Int.MinValue / 8)),
          OrderToken(List(Int.MinValue - Int.MinValue / 4)),
          OrderToken(List(Int.MinValue / 2)),
          OrderToken(List(Int.MinValue / 4))
        )
    }
    "With all bounds defined" in {
      OrderToken.evenlyDistributedValuesBetween(6, someToken(0), someToken(10)) mustEqual
        Seq(
          OrderToken(List(1)),
          OrderToken(List(2)),
          OrderToken(List(3)),
          OrderToken(List(5)),
          OrderToken(List(6)),
          OrderToken(List(7))
        )
    }
  }

  private def someToken(parts: Int*): Option[OrderToken] = Some(OrderToken(List(parts: _*)))
}
