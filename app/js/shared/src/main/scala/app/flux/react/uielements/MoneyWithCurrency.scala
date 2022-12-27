package app.flux.react.uielements

import scala.collection.immutable.Seq
import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.CurrencyValueManager
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
  )(implicit currencyValueManager: CurrencyValueManager): VdomElement = {
    money match {
      case money: DatedMoney => sum(Seq(money), correctForInflation = correctForInflation)
      case money             => render(money)
    }
  }

  def sum(
      moneySeq: Seq[DatedMoney],
      correctForInflation: Boolean = false,
  )(implicit currencyValueManager: CurrencyValueManager): VdomElement = {
    val currencies = moneySeq.map(_.currency).distinct
    val referenceSum = moneySeq.map(_.exchangedForReferenceCurrency()).sum

    if (correctForInflation) {
      val correctedReferenceSum =
        moneySeq.map(_.exchangedForReferenceCurrency(correctForInflation = true)).sum
      if (referenceSum == correctedReferenceSum) {
        sum(moneySeq, correctForInflation = false)
      } else {
        currencies match {
          case Seq(currency)
              if currency != Currency.default => // All transactions have the same foreign currency
            val foreignCurrencySum = moneySeq.sum(MoneyWithGeneralCurrency.numeric(currency))
            renderCorrectedForInflation(correctedReferenceSum, foreignCurrencySum)
          case _ => // Only reference currency or multiple currencies --> only show reference currency
            renderCorrectedForInflation(correctedReferenceSum, referenceSum)
        }
      }
    } else {
      currencies match {
        case Seq(currency)
            if currency != Currency.default => // All transactions have the same foreign currency
          val foreignCurrencySum = moneySeq.sum(MoneyWithGeneralCurrency.numeric(currency))
          renderWithReferenceCurrency(foreignCurrencySum, referenceSum)
        case _ => // Only reference currency or multiple currencies --> only show reference currency
          render(referenceSum)
      }
    }
  }

  // **************** Private helper methods ****************//
  private def render(money: Money): VdomElement = {
    <.span(money.toString)
  }

  private def renderWithReferenceCurrency(money: Money, referenceMoney: Money): VdomElement = {
    <.span(
      money.toString + " ",
      <.span(^.className := "secondary-currency", referenceMoney.toString),
    )
  }

  private def renderCorrectedForInflation(moneyCorrected: Money, originalMoney: Money): VdomElement = {
    <.span(
      <.span(^.className := "corrected-for-inflation", moneyCorrected.toString),
      " ",
      <.span(^.className := "secondary-currency", originalMoney.toString),
    )
  }
}
