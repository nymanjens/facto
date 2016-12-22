package models.accounting.money

import models.manager.{Entity, EntityManager}
import common.time.LocalDateTime
import scala.collection.immutable.Seq

/**
  * Measurements of the exchange rate of the reference currency to another (foreign) currency at a certain point in
  * time.
  *
  * This exchange rate is valid from this `date` until the `date` of the next measurement.
  *
  * ExchangeRateMeasurement entities are immutable.
  */
case class ExchangeRateMeasurement(date: LocalDateTime,
                                   foreignCurrencyCode: String,
                                   ratioReferenceToForeignCurrency: Double,
                                   idOption: Option[Long] = None) extends Entity {
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
