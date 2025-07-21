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
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.VdomArray

object MoneyWithCurrency {

  // **************** API ****************//
  def apply(
      money: Money,
      markPositiveFlow: Boolean = false,
      correctForInflation: Boolean = false,
  )(implicit currencyValueManager: CurrencyValueManager): VdomElement = {
    money match {
      case money: DatedMoney =>
        sum(Seq(money), markPositiveFlow = markPositiveFlow, correctForInflation = correctForInflation)
      case money => render(money, markPositiveFlow = markPositiveFlow)
    }
  }

  def sum(
      moneySeq: Seq[DatedMoney],
      markPositiveFlow: Boolean = false,
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
            renderCorrectedForInflation(
              correctedReferenceSum,
              foreignCurrencySum,
              markPositiveFlow = markPositiveFlow,
            )
          case _ => // Only reference currency or multiple currencies --> only show reference currency
            renderCorrectedForInflation(
              correctedReferenceSum,
              referenceSum,
              markPositiveFlow = markPositiveFlow,
            )
        }
      }
    } else {
      currencies match {
        case Seq(currency)
            if currency != Currency.default => // All transactions have the same foreign currency
          val foreignCurrencySum = moneySeq.sum(MoneyWithGeneralCurrency.numeric(currency))
          renderWithReferenceCurrency(foreignCurrencySum, referenceSum, markPositiveFlow = markPositiveFlow)
        case _ => // Only reference currency or multiple currencies --> only show reference currency
          render(referenceSum, markPositiveFlow = markPositiveFlow)
      }
    }
  }

  // **************** Private helper methods ****************//
  private def renderWithReferenceCurrency(
      money: Money,
      referenceMoney: Money,
      markPositiveFlow: Boolean,
  ): VdomElement = {
    render(
      money = money,
      markPositiveFlow = markPositiveFlow,
      extraElement = <.span(^.className := "secondary-currency", referenceMoney.toString),
    )
  }

  private def renderCorrectedForInflation(
      moneyCorrected: Money,
      originalMoney: Money,
      markPositiveFlow: Boolean,
  ): VdomElement = {
    render(
      money = moneyCorrected,
      markPositiveFlow = markPositiveFlow,
      corectedForInflation = true,
      extraElement = <.span(^.className := "secondary-currency", originalMoney.toString),
    )
  }

  private def render(
      money: Money,
      markPositiveFlow: Boolean,
      corectedForInflation: Boolean = false,
      extraElement: VdomElement = null,
  ): VdomElement = {
    <.span(
      ^^.ifThen(markPositiveFlow && money.toDouble > 0) { ^.className := "positive-money-flow" }, {
        if (corectedForInflation) {
          <.span(^.className := "corrected-for-inflation", money.toString)
        } else {
          VdomArray.empty() += money.toString
        }
      },
      <<.ifThen(extraElement != null) {
        VdomArray.empty() += " "
      },
      <<.ifThen(extraElement != null) {
        extraElement
      },
    )
  }
}
