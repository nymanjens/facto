package app.flux.react.app.transactiongroupform

import app.common.I18n
import hydro.common.LoggingUtils.LogExceptionsCallback
import hydro.common.LoggingUtils.logExceptions
import hydro.flux.react.ReactVdomUtils.^^
import app.flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

private[transactiongroupform] final class TotalFlowRestrictionInput(implicit i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps[State](props => props.defaultValue)
    .renderPS(($, props, state) =>
      logExceptions {
        def button(totalFlowRestriction: TotalFlowRestriction, label: String) = {
          <.label(
            ^^.classes(Seq("btn", "btn-default", "btn-sm")
              ++ (if (state == totalFlowRestriction) Seq("active") else Seq())),
            ^.onClick --> LogExceptionsCallback {
              $.setState(totalFlowRestriction).runNow()
              props.onChangeListener(totalFlowRestriction)
            },
            label
          )
        }
        <.div(
          ^.className := "btn-group",
          VdomAttr("data-toggle") := "buttons",
          button(TotalFlowRestriction.AnyTotal, i18n("app.any-total")),
          button(TotalFlowRestriction.ChooseTotal, i18n("app.choose-total")),
          button(TotalFlowRestriction.ZeroSum, i18n("app.zero-sum"))
        )
    })
    .build

  // **************** API ****************//
  def apply(defaultValue: TotalFlowRestriction, onChange: TotalFlowRestriction => Unit): VdomElement = {
    component(Props(defaultValue, onChange))
  }

  // **************** Private inner types ****************//
  private case class Props(defaultValue: TotalFlowRestriction, onChangeListener: TotalFlowRestriction => Unit)
  private type State = TotalFlowRestriction
}

object TotalFlowRestrictionInput {
  // **************** Public inner types ****************//
  sealed trait TotalFlowRestriction {
    def userSetsTotal: Boolean
  }
  object TotalFlowRestriction {
    object AnyTotal extends TotalFlowRestriction {
      override def userSetsTotal = false
    }
    object ChooseTotal extends TotalFlowRestriction {
      override def userSetsTotal = true
    }
    object ZeroSum extends TotalFlowRestriction {
      override def userSetsTotal = true
    }
  }
}
