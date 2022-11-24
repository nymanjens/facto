package app.flux.react.uielements

import scala.collection.immutable.Seq
import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.ExchangeRateManager
import app.common.money.Money
import app.common.money.MoneyWithGeneralCurrency
import hydro.common.GuavaReplacement.Preconditions
import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object MoneyWithCurrency {

  // **************** API ****************//
  def apply(
      money: Money,
      correctForInflation: Boolean = false,
  )(implicit exchangeRateManager: ExchangeRateManager): VdomElement = {
    money match {
      case money: DatedMoney if money.currency != Currency.default || correctForInflation =>
        val referenceMoney = {
          money.exchangedForReferenceCurrency(correctForInflation = correctForInflation)
        }
        render(primary = money, secondary = referenceMoney, secondaryClass = "reference-currency")
      case money =>
        render(money)
    }
  }

  def sum(moneySeq: Seq[DatedMoney])(implicit exchangeRateManager: ExchangeRateManager): VdomElement = {
    val currencies = moneySeq.map(_.currency).distinct
    val referenceSum = moneySeq.map(_.exchangedForReferenceCurrency()).sum

    currencies match {
      case Seq(Currency.default) =>
        render(referenceSum)
      case Seq(currency) if currency != Currency.default => // All transactions have the same foreign currency
        val foreignCurrencySum = moneySeq.sum(MoneyWithGeneralCurrency.numeric(currency))
        render(primary = foreignCurrencySum, secondary = referenceSum, secondaryClass = "reference-currency")
      case _ => // Multiple currencies --> only show reference currency
        render(referenceSum)
    }
  }

  // **************** Private helper methods ****************//
  private def render(money: Money): VdomElement = {
    <.span(money.toString)
  }
  private def render(primary: Money, secondary: Money, secondaryClass: String): VdomElement = {
    <.span(
      primary.toString + " ",
      <.span(^.className := secondaryClass, secondary.toString),
    )
  }
}
