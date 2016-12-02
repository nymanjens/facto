package models.accounting.money

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import com.google.common.collect.{ImmutableMultiset, Multiset}
import common.time.Clock
import common.CollectionUtils.toListMap
import models.manager.{Entity, EntityManager}
import common.time.LocalDateTime

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.collection.immutable.ListMap

/**
  * Measurements of the exchange rate of the reference currency to another (foreign) currency at a certain point in
  * time.
  *
  * This exchange rate is valid from this `date` until the `date` of the next measurement.
  *
  * ExchangeRateMeasurement entities are immutable.
  */
case class ExchangeRateMeasurement(date: LocalDateTime,
                                   private val foreignCurrencyCode: String,
                                   ratioReferenceToForeignCurrency: Double,
                                   idOption: Option[Long] = None) extends Entity[ExchangeRateMeasurement] {
  require(!foreignCurrencyCode.isEmpty)
  require(ratioReferenceToForeignCurrency > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def foreignCurrency: Currency = Currency.of(foreignCurrencyCode)
}

object ExchangeRateMeasurement {
  def tupled = (this.apply _).tupled

  trait Manager extends EntityManager[ExchangeRateMeasurement] {

    def fetchAll(currency: Currency): Seq[ExchangeRateMeasurement]
  }
}
