package flux.react.uielements

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{^^, <<}
import models.accounting.money.{Currency, DatedMoney, ExchangeRateManager, Money}

import scala.collection.immutable.Seq

object MoneyWithCurrency {
  private case class Props(money: Money, exchangeRateManager: ExchangeRateManager)
  private val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      props.money match {
        case money: DatedMoney if money.currency != Currency.default =>
          val referenceMoney = money.exchangedForReferenceCurrency(props.exchangeRateManager)
          <.span(
            money.toString + " ",
            <.span(^.className := "reference-currency", referenceMoney.toString)
          )
        case money =>
          <.span(money.toString)
      }
    })
    .build

  def apply(money: Money)(implicit exchangeRateManager: ExchangeRateManager): VdomElement = {
    component(Props(money, exchangeRateManager))
  }
}
