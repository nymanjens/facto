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
    .renderPS(($, props, state) => {
      def button(totalFlowRestriction: TotalFlowRestriction, label: String) = {
        <.label(
          ^^.classes(Seq("btn", "btn-default", "btn-sm")
            ++ (if (state == totalFlowRestriction) Seq("active") else Seq())),
          <.input(
            ^.tpe := "radio",
            ^.autoComplete := "off",
            ^^.ifThen(state == totalFlowRestriction) {
              ^.checked := true
            },
            ^.onChange --> Callback(),
            ^.onClick --> LogExceptionsCallback {
              $.setState(totalFlowRestriction).runNow()
              props.onChange(totalFlowRestriction)
            }
          ),
          label
        )
      }
      <.div(
        ^.className := "btn-group",
        ReactAttr("data-toggle") := "buttons",
        button(TotalFlowRestriction.AnyTotal, i18n("facto.any-total")),
        button(TotalFlowRestriction.ChooseTotal, i18n("facto.choose-total")),
        button(TotalFlowRestriction.ZeroSum, i18n("facto.zero-sum"))
      )
    }).build

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
    object ChooseTotal extends TotalFlowRestriction
    object ZeroSum extends TotalFlowRestriction
  }
}