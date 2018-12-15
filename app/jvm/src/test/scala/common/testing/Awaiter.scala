package common.testing

import scala.language.reflectiveCalls

object Awaiter {

  def expectEventually: EventuallyAwaiter = new EventuallyAwaiter
  def expectConsistently: ConsistentlyAwaiter = new ConsistentlyAwaiter

  sealed abstract class AwaiterWithType {
    protected def verb: String
    protected def expectCondition(condition: => Boolean, onFail: => Unit): Unit

    def equal[T](a: => T, b: => T): Unit = {
      expectCondition(a == b, throw new AssertionError(s"Expected $a == $b to be $verb true"))
    }

    def nonEmpty[T](iterable: => Iterable[T]): Unit = {
      expectCondition(
        iterable.nonEmpty,
        throw new AssertionError(s"Expected given iterable to be $verb non-empty"))
    }
  }
  final class EventuallyAwaiter extends AwaiterWithType {
    override protected def verb = "eventually"
    override protected def expectCondition(condition: => Boolean, onFail: => Unit) = {
      var cycleCount = 0
      while (cycleCount < 100 && !condition) {
        Thread.sleep(5)
        cycleCount += 1
      }
      if (!condition) {
        onFail
        throw new AssertionError(s"expect $verb timed out")
      }
    }
  }
  final class ConsistentlyAwaiter extends AwaiterWithType {
    override protected def verb = "consistently"
    override protected def expectCondition(condition: => Boolean, onFail: => Unit) = {
      throw new UnsupportedOperationException("expectCondition() not yet implemented")
    }
  }
}
