package common

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.async.Async.{async, await}
import common.Listenable.WritableListenable
import common.testing.Awaiter
import utest.{TestSuite, _}

import scala.concurrent.{Await, Promise}
import scala2js.Converters._

object ListenableTest extends TestSuite {

  override def tests = TestSuite {

    "WritableListenable" - {
      val listenable: WritableListenable[Int] = WritableListenable(123)

      "get" - {
        listenable.get ==> 123
      }
      "set" - {
        listenable.set(456)
        listenable.get ==> 456
      }
      "calls listener when value changes" - async {
        val promise = Promise[Int]()
        listenable.registerListener(newValue => promise.success(newValue))

        listenable.set(456)

        await(Awaiter.expectEventuallyComplete(promise.future, 456))
      }
      "doesn't call listener when value stays the same" - async {
        val promise = Promise[Int]()
        listenable.registerListener(newValue => promise.success(newValue))

        listenable.set(123)

        await(Awaiter.expectNeverComplete(promise.future))
      }
    }
  }
}
