package common.publisher

import net.jcip.annotations.GuardedBy
import org.reactivestreams.{Publisher, Subscriber, Subscription}

import scala.collection.immutable.Seq

object Publishers {

  def map[From, To](delegate: Publisher[From], mappingFunction: From => To): Publisher[To] =
    new MappingPublisher(delegate, mappingFunction)

  def delayMessagesUntilFirstSubscriber[T](delegate: Publisher[T]): Publisher[T] =
    new ReplayingPublisher(delegate)

  private final class MappingPublisher[From, To](delegate: Publisher[From], mappingFunction: From => To)
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

  /** TODO */
  private final class ReplayingPublisher[T](delegate: Publisher[T]) extends Publisher[T] {
    private val accumulatingSubscriber = new AccumulatingSubscriber()
    delegate.subscribe(accumulatingSubscriber)

    override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
      delegate.subscribe(subscriber)

      for (message <- accumulatingSubscriber.accumulatedMessages) {
        subscriber.onNext(message)
      }
      accumulatingSubscriber.cancelSubscription()
    }

    private final class AccumulatingSubscriber extends Subscriber[T] {
      @volatile private var subscription: Option[Subscription] = None
      private val accumulatedMessagesLock = new Object
      @GuardedBy("accumulatedMessagesLock") private var _accumulatedMessages: Seq[T] = Seq()

      override def onSubscribe(subscription: Subscription): Unit = {
        this.subscription = Some(subscription)
      }
      override def onNext(message: T): Unit = accumulatedMessagesLock.synchronized {
        _accumulatedMessages = _accumulatedMessages :+ message
      }
      override def onError(t: Throwable): Unit = {}
      override def onComplete(): Unit = {}

      def accumulatedMessages: Seq[T] = accumulatedMessagesLock.synchronized {
        _accumulatedMessages
      }

      def cancelSubscription(): Unit = {
        require(subscription.isDefined, "Expected subscription to be set")
        subscription.get.cancel()
      }
    }
  }
}
