package app.models.money

import scala.concurrent.ExecutionContext.Implicits.global
import net.jcip.annotations.GuardedBy

import scala.collection.immutable.TreeMap
import com.google.inject.Singleton
import app.api.ScalaJsApi.HydroPushSocketPacket.EntityModificationsWithToken
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import app.common.money.Currency
import app.common.money.CurrencyValueManager
import app.common.time.DatedMonth
import app.models.access.JvmEntityAccess
import com.google.inject.Inject
import hydro.common.time.Clock
import hydro.common.time.JavaTimeImplicits._
import hydro.models.modification.EntityModification
import hydro.common.time.LocalDateTime

import java.time.Duration
import javax.annotation.Nullable
import scala.collection.immutable.Seq
import scala.collection.SortedMap
import scala.collection.mutable
import scala.concurrent.Future

@Singleton
final class JvmCurrencyValueManager @Inject() (implicit entityAccess: JvmEntityAccess, clock: Clock)
    extends CurrencyValueManager {
  private val measurementsCacheWriteLock = new Object
  @GuardedBy("measurementsCacheWriteLock") @volatile @Nullable private var measurementsCache
      : Map[Currency, SortedMap[LocalDateTime, Double]] = null
  entityAccess.entityModificationPublisher.subscribe(EntityModificationSubscriber)

  // **************** Implementation of CurrencyValueManager trait ****************//
  protected[app] override def ratioReferenceToForeignCurrencyDataPoints(
      currency: Currency
  ): SortedMap[LocalDateTime, Double] = {
    if (measurementsCache == null) {
      measurementsCacheWriteLock.synchronized {
        measurementsCache = recalculateMeasurementsCache()
      }
    }
    measurementsCache.getOrElse(currency, SortedMap())
  }

  // **************** Private helper methods ****************//
  private def recalculateMeasurementsCache(): Map[Currency, SortedMap[LocalDateTime, Double]] = {
    val mapBuilder =
      mutable.Map[Currency, mutable.Builder[(LocalDateTime, Double), TreeMap[LocalDateTime, Double]]]()
    for (measurement <- entityAccess.newQuerySync[ExchangeRateMeasurement]().data()) {
      val currency = measurement.foreignCurrency
      if (!(mapBuilder contains currency)) {
        mapBuilder(currency) = TreeMap.newBuilder[LocalDateTime, Double]
      }
      mapBuilder(currency) += (measurement.date -> measurement.ratioReferenceToForeignCurrency)
    }
    mapBuilder.toStream.map { case (k, v) => k -> v.result() }.toMap
  }
  // **************** Inner type definitions ****************//
  private object EntityModificationSubscriber extends Subscriber[EntityModificationsWithToken] {
    override def onSubscribe(s: Subscription): Unit = {
      s.request(Long.MaxValue)
    }
    override def onNext(t: EntityModificationsWithToken): Unit = {
      for (modification <- t.modifications) {
        modification.entityType match {
          case ExchangeRateMeasurement.Type =>
            Future {
              measurementsCacheWriteLock.synchronized {
                measurementsCache = recalculateMeasurementsCache()
              }
            }

          case _ => false // do nothing
        }
      }
    }
    override def onError(t: Throwable): Unit = {}
    override def onComplete(): Unit = {}
  }
}
