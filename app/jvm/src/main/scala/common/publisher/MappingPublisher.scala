package common.publisher

import org.reactivestreams.{Publisher, Subscriber, Subscription}

final class MappingPublisher[From, To](delegate: Publisher[From], mappingFunction: From => To)
    extends Publisher[To] {
  override def subscribe(outerSubscriber: Subscriber[_ >: To]): Unit = {
    delegate.subscribe(new Subscriber[From] {
      override def onSubscribe(subscription: Subscription): Unit = outerSubscriber.onSubscribe(subscription)
      override def onNext(t: From): Unit = outerSubscriber.onNext(mappingFunction(t))
      override def onError(t: Throwable): Unit = outerSubscriber.onError(t)
      override def onComplete(): Unit = outerSubscriber.onComplete()
    })
  }
}
