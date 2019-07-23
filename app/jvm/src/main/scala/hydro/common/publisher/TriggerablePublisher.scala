package hydro.common.publisher

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import scala.collection.JavaConverters._

final class TriggerablePublisher[T] extends Publisher[T] {
  private val subscribers: java.util.List[Subscriber[_ >: T]] =
    new java.util.concurrent.CopyOnWriteArrayList()

  override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
    subscribers.add(subscriber)
    subscriber.onSubscribe(new Subscription {
      override def request(n: Long): Unit = {}
      override def cancel(): Unit = {
        subscribers.remove(subscriber)
      }
    })
  }

  def trigger(value: T): Unit = {
    for (s <- subscribers.asScala) {
      s.onNext(value)
    }
  }

  def complete(): Unit = {
    for (s <- subscribers.asScala) {
      s.onComplete()
    }
  }
}
