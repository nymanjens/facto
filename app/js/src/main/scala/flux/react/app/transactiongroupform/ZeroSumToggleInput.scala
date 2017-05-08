package flux.react.app.transactiongroupform

import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import flux.react.ReactVdomUtils.^^
import flux.react.app.transactiongroupform.ZeroSumToggleInput.ZeroSumState
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

private[transactiongroupform] final class ZeroSumToggleInput(implicit i18n: I18n) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState_P[State](props => props.defaultValue)
    .renderPS(($, props, state) =>
      <.div(
        ^.className := "btn-group",
        ReactAttr("data-toggle") := "buttons",
        <.label(
          ^^.classes(Seq("btn", "btn-default", "btn-sm")
            ++ (if (state == ZeroSumState.AnyTotal) Seq("active") else Seq())),
          <.input(
            ^.tpe := "radio",
            ^.name := "zeroSum",
            ^.value := "false",
            ^.autoComplete := "off",
            ^^.ifThen(state == ZeroSumState.AnyTotal) {
              ^.checked := true
            },
            ^.onChange --> Callback(),
            ^.onClick --> LogExceptionsCallback {
              $.setState(ZeroSumState.AnyTotal).runNow()
              props.onChange(ZeroSumState.AnyTotal)
            }
          ),
          i18n("facto.any-total")
        ),
        <.label(
          ^^.classes(Seq("btn", "btn-default", "btn-sm")
            ++ (if (state == ZeroSumState.ZeroSum) Seq("active") else Seq())),
          <.input(
            ^.tpe := "radio",
            ^.name := "zeroSum",
            ^.value := "true",
            ^.autoComplete := "off",
            ^^.ifThen(state == ZeroSumState.ZeroSum) {
              ^.checked := true
            },
            ^.onChange --> Callback(),
            ^.onClick --> LogExceptionsCallback {
              $.setState(ZeroSumState.ZeroSum).runNow()
              props.onChange(ZeroSumState.ZeroSum)
            }
          ),
          i18n("facto.zero-sum")
        )
      )
    ).build

  // **************** API ****************//
  def apply(defaultValue: ZeroSumState, onChange: ZeroSumState => Unit): ReactElement = {
    component(Props(defaultValue, onChange))
  }

  // **************** Private inner types ****************//
  private case class Props(defaultValue: ZeroSumState, onChange: ZeroSumState => Unit)
  private type State = ZeroSumState
}

object ZeroSumToggleInput {
  // **************** Public inner types ****************//
  sealed trait ZeroSumState
  object ZeroSumState {
    object AnyTotal extends ZeroSumState
    object ZeroSum extends ZeroSumState
  }
}