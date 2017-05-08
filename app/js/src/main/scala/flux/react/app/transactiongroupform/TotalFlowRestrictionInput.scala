package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import flux.react.ReactVdomUtils.^^
import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

private[transactiongroupform] final class TotalFlowRestrictionInput(implicit i18n: I18n) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState_P[State](props => props.defaultValue)
    .renderPS(($, props, state) =>
      <.div(
        ^.className := "btn-group",
        ReactAttr("data-toggle") := "buttons",
        <.label(
          ^^.classes(Seq("btn", "btn-default", "btn-sm")
            ++ (if (state == TotalFlowRestriction.AnyTotal) Seq("active") else Seq())),
          <.input(
            ^.tpe := "radio",
            ^.autoComplete := "off",
            ^^.ifThen(state == TotalFlowRestriction.AnyTotal) {
              ^.checked := true
            },
            ^.onChange --> Callback(),
            ^.onClick --> LogExceptionsCallback {
              $.setState(TotalFlowRestriction.AnyTotal).runNow()
              props.onChange(TotalFlowRestriction.AnyTotal)
            }
          ),
          i18n("facto.any-total")
        ),
        <.label(
          ^^.classes(Seq("btn", "btn-default", "btn-sm")
            ++ (if (state == TotalFlowRestriction.ZeroSum) Seq("active") else Seq())),
          <.input(
            ^.tpe := "radio",
            ^.autoComplete := "off",
            ^^.ifThen(state == TotalFlowRestriction.ZeroSum) {
              ^.checked := true
            },
            ^.onChange --> Callback(),
            ^.onClick --> LogExceptionsCallback {
              $.setState(TotalFlowRestriction.ZeroSum).runNow()
              props.onChange(TotalFlowRestriction.ZeroSum)
            }
          ),
          i18n("facto.zero-sum")
        )
      )
    ).build

  // **************** API ****************//
  def apply(defaultValue: TotalFlowRestriction, onChange: TotalFlowRestriction => Unit): ReactElement = {
    component(Props(defaultValue, onChange))
  }

  // **************** Private inner types ****************//
  private case class Props(defaultValue: TotalFlowRestriction, onChange: TotalFlowRestriction => Unit)
  private type State = TotalFlowRestriction
}

object TotalFlowRestrictionInput {
  // **************** Public inner types ****************//
  sealed trait TotalFlowRestriction
  object TotalFlowRestriction {
    object AnyTotal extends TotalFlowRestriction
    object ZeroSum extends TotalFlowRestriction
  }
}