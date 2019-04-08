package hydro.common.publisher

import net.jcip.annotations.GuardedBy
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import scala.collection.immutable.Seq

/** Utility methods for working with reactivestreams Publishers. */
object Publishers {

  /**
    * Returns the same publisher as the given one, except that the given `mappingFunction` is applied to all
    * messages.
    */
  def map[From, To](delegate: Publisher[From], mappingFunction: From => To): Publisher[To] =
    new MappingPublisher(delegate, mappingFunction)

  def combine[T](publishers: Publisher[_ <: T]*): Publisher[T] =
    new CombiningPublisher[T](publishers.toVector)

  /**
    * Returns a new publisher that is the same as the given publisher, except that the messages posted by the
    * given publisher are stored and replayed when the first subscriber is added to the returned subscriber.
    */
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

  private final class CombiningPublisher[T](delegatePublishers: Seq[Publisher[_ <: T]]) extends Publisher[T] {
    override def subscribe(outerSubscriber: Subscriber[_ >: T]): Unit = {
      for (delegate <- delegatePublishers)
        delegate.subscribe(new Subscriber[T] {
          override def onSubscribe(subscription: Subscription): Unit =
            outerSubscriber.onSubscribe(subscription)
          override def onNext(t: T): Unit = outerSubscriber.onNext(t)
          override def onError(t: Throwable): Unit = outerSubscriber.onError(t)
          override def onComplete(): Unit = outerSubscriber.onComplete()
        })
    }
  }

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
