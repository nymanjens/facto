package app.models.money

import app.common.money.Currency
import app.models.modification.EntityType
import hydro.common.time.LocalDateTime
import hydro.models.Entity

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
                                   idOption: Option[Long] = None)
    extends Entity {
  require(!foreignCurrencyCode.isEmpty)
  require(ratioReferenceToForeignCurrency > 0)
  for (idVal <- idOption) require(idVal > 0)

  override def withId(id: Long) = copy(idOption = Some(id))

  def foreignCurrency: Currency = Currency.of(foreignCurrencyCode)
}

object ExchangeRateMeasurement {
  implicit val Type: EntityType[ExchangeRateMeasurement] = EntityType()

  def tupled = (this.apply _).tupled
}
