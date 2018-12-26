package common

import common.Listenable.WritableListenable
import common.testing.Awaiter
import app.scala2js.Converters._
import utest.TestSuite
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

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

        await(Awaiter.expectEventually.complete(promise.future, 456))
      }
      "doesn't call listener when value stays the same" - async {
        val promise = Promise[Int]()
        listenable.registerListener(newValue => promise.success(newValue))

        listenable.set(123)

        await(Awaiter.expectConsistently.neverComplete(promise.future))
      }
    }
  }
}
