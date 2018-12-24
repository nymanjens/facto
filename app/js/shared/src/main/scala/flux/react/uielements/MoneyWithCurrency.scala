package flux.react.uielements

import common.money.Currency
import common.money.DatedMoney
import common.money.ExchangeRateManager
import common.money.Money
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object MoneyWithCurrency {
  private case class Props(money: Money, exchangeRateManager: ExchangeRateManager)
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
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
