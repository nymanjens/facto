package flux.react.app.transactiongroupform

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.^^

import scala.collection.immutable.Seq

private[transactiongroupform] object HalfPanel {
  private case class Props(title: ReactElement, panelClasses: Seq[String], closeButtonCallback: Option[Callback])
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderPC((_, props, children) =>
      <.div(^^.classes("col-lg-6" +: props.panelClasses),
        <.div(^^.classes("panel panel-default"),
          <.div(^^.classes("panel-heading"),
            props.title,
            ^^.ifThen(props.closeButtonCallback.isDefined) {
              <.div(
                ^.className := "pull-right",
                <.button(
                  ^.tpe := "button",
                  ^.className := "btn btn-default btn-xs",
                  ^.onClick --> props.closeButtonCallback.get,
                  <.i(
                    ^.className := "fa  fa-times fa-fw"
                  )
                )
              )
            }
          ),
          <.div(^^.classes("panel-body"),
            children
          )
        )
      )
    ).build

  def apply(title: ReactElement,
            panelClasses: Seq[String] = Seq(),
            closeButtonCallback: Option[Callback] = None)(children: ReactNode*): ReactElement = {
    component(Props(title, panelClasses, closeButtonCallback), children: _*)
  }
}
