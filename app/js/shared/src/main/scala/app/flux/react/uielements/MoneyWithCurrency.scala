package app.flux.react.uielements

import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.ExchangeRateManager
import app.common.money.Money
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class MoneyWithCurrency(implicit
    exchangeRateManager: ExchangeRateManager,
) extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      money: Money,
      correctForInflation: Boolean = false,
  ): VdomElement = {
    component(Props(money, correctForInflation))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      money: Money,
      correctForInflation: Boolean,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = {
      props.money match {
        case money: DatedMoney if money.currency != Currency.default || props.correctForInflation =>
          val referenceMoney =
            money.exchangedForReferenceCurrency(correctForInflation = props.correctForInflation)
          <.span(
            money.toString + " ",
            <.span(^.className := "reference-currency", referenceMoney.toString),
          )
        case money =>
          <.span(money.toString)
      }
    }
  }
}
