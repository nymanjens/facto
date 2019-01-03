package hydro.common

import hydro.common.Listenable.ListenableMap
import hydro.common.Listenable.WritableListenable
import hydro.common.testing.Awaiter
import utest.TestSuite
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ListenableTest extends TestSuite {

  override def tests = TestSuite {

    "mergeWith()" - {
      val l1 = WritableListenable(123)
      val l2 = WritableListenable(456)
      val sum = Listenable.mergeWith[Int](_ + _)(l1, l2)

      "get" - async {
        sum.get ==> 579

        l2.set(-1)
        await(Awaiter.expectEventually.equal(sum.get, 122))
      }
      "calls listener when value changes" - async {
        val promise = Promise[Int]()
        sum.registerListener(newValue => promise.success(newValue))

        l2.set(-1)

        await(Awaiter.expectEventually.complete(promise.future, 122))
      }
      "only registers listener if listened to" - {
        val listener1: Listenable.Listener[Int] = _ => {}
        val listener2: Listenable.Listener[Int] = _ => {}

        l1.hasListeners ==> false
        sum.registerListener(listener1)
        l1.hasListeners ==> true
        sum.registerListener(listener2)
        l1.hasListeners ==> true
        sum.deregisterListener(listener1)
        l1.hasListeners ==> true
        sum.deregisterListener(listener2)
        l1.hasListeners ==> false
      }
    }

    "fromFuture()" - {
      "from unresolved future" - {
        val listenableBackingPromise = Promise[Int]()
        val listenable: Listenable[Option[Int]] = Listenable.fromFuture(listenableBackingPromise.future)

        "get" - async {
          listenable.get ==> None

          listenableBackingPromise.success(123)

          await(Awaiter.expectEventually.equal(listenable.get, Some(123)))
        }
        "calls listener when value changes" - async {
          val promise = Promise[Option[Int]]()
          listenable.registerListener(newValue => promise.success(newValue))

          listenableBackingPromise.success(777)

          await(Awaiter.expectEventually.complete(promise.future, Some(777)))
        }
      }
      "from completed future" - {
        val listenable: Listenable[Option[Int]] = Listenable.fromFuture(Future.successful(123))

        "get" - async {
          listenable.get ==> Some(123)
        }
      }
    }

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

    "ListenableMap" - {
      val map = ListenableMap[Int, String]()
      map.put(1, "one")

      "get" - {
        map.get ==> Map(1 -> "one")
      }
      "contains" - {
        (map contains 1) ==> true
        (map contains 2) ==> false
      }
      "put" - {
        map.put(2, "two")
        map.get ==> Map(1 -> "one", 2 -> "two")
      }
      "apply" - {
        map(1) ==> "one"
      }
      "calls listener when value changes" - async {
        val promise = Promise[Map[Int, String]]()
        map.registerListener(newValue => promise.success(newValue))

        map.put(1, "ONE")

        await(Awaiter.expectEventually.complete(promise.future, Map(1 -> "ONE")))
      }
      "doesn't call listener when value stays the same" - async {
        val promise = Promise[Map[Int, String]]()
        map.registerListener(newValue => promise.success(newValue))

        map.put(1, "one")

        await(Awaiter.expectConsistently.neverComplete(promise.future))
      }
    }
    "map" - {
      val delegate = WritableListenable("abc")
      val mapped = delegate.map(_.length)

      "get" - async {
        mapped.get ==> 3

        delegate.set("abcde")
        await(Awaiter.expectEventually.equal(mapped.get, 5))
      }
      "calls listener when value changes" - async {
        val promise = Promise[Int]()
        mapped.registerListener(newValue => promise.success(newValue))

        delegate.set("abcdef")

        await(Awaiter.expectEventually.complete(promise.future, 6))
      }
      "only registers listener if listened to" - {
        val listener1: Listenable.Listener[Int] = _ => {}
        val listener2: Listenable.Listener[Int] = _ => {}

        delegate.hasListeners ==> false
        mapped.registerListener(listener1)
        delegate.hasListeners ==> true
        mapped.registerListener(listener2)
        delegate.hasListeners ==> true
        mapped.deregisterListener(listener1)
        delegate.hasListeners ==> true
        mapped.deregisterListener(listener2)
        delegate.hasListeners ==> false
      }
    }

    "flatMap" - {
      val delegate = WritableListenable("abc")
      val mapOffset = WritableListenable(2)
      val mapped = delegate.flatMap(value => mapOffset.map(_ + value.length))

      "get" - async {
        mapped.get ==> 5

        delegate.set("abcde")
        await(Awaiter.expectEventually.equal(mapped.get, 7))

        mapOffset.set(3)
        await(Awaiter.expectEventually.equal(mapped.get, 8))
      }
      "calls listener when delegate changes" - async {
        val promise = Promise[Int]()
        mapped.registerListener(newValue => promise.success(newValue))

        delegate.set("abcdef")

        await(Awaiter.expectEventually.complete(promise.future, 8))
      }
      "calls listener when mapOffset changes" - async {
        val promise = Promise[Int]()
        mapped.registerListener(newValue => promise.success(newValue))

        mapOffset.set(-1)

        await(Awaiter.expectEventually.complete(promise.future, 2))
      }
      "doesn't call listener when value stays the same" - async {
        val promise = Promise[Int]()
        mapped.registerListener(newValue => promise.success(newValue))

        delegate.set("def")

        await(Awaiter.expectConsistently.neverComplete(promise.future))
      }
      "only registers listener if listened to" - {
        val listener1: Listenable.Listener[Int] = _ => {}
        val listener2: Listenable.Listener[Int] = _ => {}
        mapped.get

        delegate.hasListeners ==> false
        mapOffset.hasListeners ==> false
        mapped.registerListener(listener1)
        delegate.hasListeners ==> true
        mapOffset.hasListeners ==> true
        mapped.registerListener(listener2)
        delegate.hasListeners ==> true
        mapOffset.hasListeners ==> true
        mapped.deregisterListener(listener1)
        delegate.hasListeners ==> true
        mapOffset.hasListeners ==> true
        mapped.deregisterListener(listener2)
        delegate.hasListeners ==> false
        mapOffset.hasListeners ==> false
      }

      "only unregisters if mapping function gets called again" - {
        val listener1: Listenable.Listener[Int] = _ => {}

        mapOffset.hasListeners ==> false

        mapped.registerListener(listener1)
        delegate.set("x")

        mapOffset.hasListeners ==> true
        mapped.deregisterListener(listener1)
        mapOffset.hasListeners ==> false
      }
    }
  }
}
