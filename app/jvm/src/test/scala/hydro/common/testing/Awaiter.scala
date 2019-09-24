package hydro.common.testing

import org.specs2.matcher.MatchResult
import org.specs2.matcher.MatchSuccess
import org.specs2.matcher.MustExpectable

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.language.reflectiveCalls
import scala.util.Success

object Awaiter {

  def expectEventually: EventuallyAwaiter = new EventuallyAwaiter
  def expectConsistently: ConsistentlyAwaiter = new ConsistentlyAwaiter

  sealed abstract class AwaiterWithType {
    protected def verb: String
    protected def expectCondition(condition: => Boolean, failureMessage: => String): MatchResult[Any]

    def equal[T](a: => T, b: => T): MatchResult[Any] = {
      expectCondition(a == b, s"Expected $a == $b to be $verb true")
    }

    def nonEmpty[T](iterable: => Iterable[T]): MatchResult[Any] = {
      expectCondition(iterable.nonEmpty, s"Expected given iterable to be $verb non-empty")
    }
  }
  final class EventuallyAwaiter extends AwaiterWithType {
    override protected def verb = "eventually"
    override protected def expectCondition(
        condition: => Boolean,
        failureMessage: => String,
    ): MatchResult[Any] = {
      var cycleCount = 0
      while (cycleCount < 100 && !condition) {
        Thread.sleep(5)
        cycleCount += 1
      }
      if (!condition) {
        throw new AssertionError(failureMessage)
      }
      MustExpectable(condition) mustEqual true
    }

    def complete[T](future: Future[T], expected: T = null): MatchResult[Any] = {
      expectCondition(future.isCompleted, s"future completion timed out (expected $expected)")

      MustExpectable(future.value.get) mustEqual Success(expected)
    }
  }
  final class ConsistentlyAwaiter extends AwaiterWithType {
    override protected def verb = "consistently"
    override protected def expectCondition(
        condition: => Boolean,
        failureMessage: => String,
    ): MatchResult[Any] = {
      for (i <- 1 to 50) {
        if (!condition) {
          throw new AssertionError(failureMessage)
        }
        Thread.sleep(5)
      }

      MustExpectable(condition) mustEqual true
    }

    def neverComplete(future: Future[_]): MatchResult[Any] = {
      expectCondition(
        !future.isCompleted,
        s"Expected future that never completes, but completed with tryValue ${future.value.get}")
    }
  }
}
