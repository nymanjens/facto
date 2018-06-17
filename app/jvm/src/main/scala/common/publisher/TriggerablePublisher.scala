package common.publisher

import scala.collection.JavaConverters._
import org.reactivestreams.{Publisher, Subscriber, Subscription}

final class TriggerablePublisher[T] extends Publisher[T] {
  private val subscribers: java.util.List[Subscriber[_ >: T]] =
    new java.util.concurrent.CopyOnWriteArrayList()

  override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
    subscribers.add(subscriber)
    subscriber.onSubscribe(new Subscription {
      override def request(n: Long): Unit = {}
      override def cancel(): Unit = {
        println("!!!!!!!!!!! CANCEL")
        subscribers.remove(subscriber)
      }
    })
  }

  def trigger(value: T): Unit = {
    println(s"!!!!!!!!! run(): subscribers = ${subscribers.size()}  - $subscribers")
    for (s <- subscribers.asScala) {
      s.onNext(value)
    }
  }
}
