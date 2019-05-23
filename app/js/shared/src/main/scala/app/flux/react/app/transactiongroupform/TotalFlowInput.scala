package app.flux.react.app.transactiongroupform

import hydro.common.I18n
import app.common.money.Currency
import app.common.money.ExchangeRateManager
import app.common.money.Money
import app.common.money.ReferenceMoney
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

private[transactiongroupform] final class TotalFlowInput(implicit i18n: I18n,
                                                         clock: Clock,
                                                         exchangeRateManager: ExchangeRateManager) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps(props =>
      logExceptions {
        State(valueString = props.forceValue match {
          case Some(forceValue) => forceValue.formatFloat
          case None             => props.defaultValue.formatFloat
        })
    })
    .renderPS(($, props, state) =>
      logExceptions {
        def onChange(e: ReactEventFromInput): Callback = LogExceptionsCallback {
          val newString = e.target.value
          val newValue = ReferenceMoney(Money.floatStringToCents(newString) getOrElse 0)
          val oldValue = state.parsedValueOrDefault
          if (oldValue != newValue) {
            props.onChangeListener(newValue)
          }
          $.modState(_.copy(valueString = newString)).runNow()
        }

        val foreignMoney = props.foreignCurrency map { foreignCurrency =>
          val datedMoney = state.parsedValueOrDefault.withDate(clock.now)
          datedMoney.exchangedForCurrency(foreignCurrency)
        }

        Bootstrap.FormInline(tag = <.span)(
          ^.className := "total-flow-form",
          Bootstrap.InputGroup(
            Bootstrap.InputGroupAddon(
              i18n("app.total") + ":"
            ),
            Bootstrap.InputGroupAddon(
              <.i(^.className := Currency.default.iconClass)
            ),
            <.input(
              ^.tpe := "text",
              ^.className := "form-control",
              ^.autoComplete := "off",
              ^.name := getClass.getSimpleName,
              ^.value := state.valueString,
              ^.disabled := props.forceValue.isDefined,
              ^.onChange ==> onChange
            ),
            ^^.ifDefined(foreignMoney) { money =>
              Bootstrap.InputGroupAddon(
                <.i(^.className := money.currency.iconClass),
                " ",
                money.formatFloat
              )
            }
          )
        )
    })
    .componentWillReceiveProps(scope =>
      LogExceptionsCallback {
        scope.nextProps.forceValue match {
          case Some(forceValue) =>
            scope.modState(_.copy(valueString = forceValue.formatFloat)).runNow()
          case None =>
        }
        // Not calling listener here because we only have a listener in the props. The parent
        // component doesn't need to be notified of changes it made to the props.
    })
    .build

  // **************** API ****************//
  def apply(defaultValue: ReferenceMoney = ReferenceMoney(0),
            forceValue: Option[ReferenceMoney],
            foreignCurrency: Option[Currency],
            onChange: ReferenceMoney => Unit): VdomElement = {
    component(Props(defaultValue, forceValue, foreignCurrency, onChange))
  }

  // **************** Private inner types ****************//
  case class Props(defaultValue: ReferenceMoney,
                   forceValue: Option[ReferenceMoney],
                   foreignCurrency: Option[Currency],
                   onChangeListener: ReferenceMoney => Unit)

  case class State(valueString: String) {
    def parsedValueOrDefault: ReferenceMoney =
      ReferenceMoney(Money.floatStringToCents(valueString) getOrElse 0)
  }
}
