package app.flux.react.uielements

import app.common.money.Currency
import app.common.money.DatedMoney
import app.common.money.ExchangeRateManager
import app.common.money.Money
import hydro.flux.react.HydroReactComponent
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object MoneyWithCurrency extends HydroReactComponent.Stateless {

  // **************** API ****************//
  def apply(
      money: Money,
      correctForInflation: Boolean = true,
  )(implicit exchangeRateManager: ExchangeRateManager): VdomElement = {
    component(Props(money, correctForInflation, exchangeRateManager))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val statelessConfig = StatelessComponentConfig(backendConstructor = new Backend(_))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      money: Money,
      correctForInflation: Boolean,
      exchangeRateManager: ExchangeRateManager,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {
    override def render(props: Props, state: State): VdomElement = {
      implicit val _: ExchangeRateManager = props.exchangeRateManager

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
