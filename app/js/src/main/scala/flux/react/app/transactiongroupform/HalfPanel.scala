package flux.react.app.transactiongroupform

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{^^, <<}

import scala.collection.immutable.Seq

private[transactiongroupform] object HalfPanel {
  private val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .renderPC(
      (_, props, children) =>
        <.div(
          ^^.classes("col-lg-6" +: props.panelClasses),
          <.div(
            ^^.classes("panel panel-default"),
            <.div(
              ^^.classes("panel-heading"),
              props.title,
              <<.ifThen(props.closeButtonCallback.isDefined) {
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
            <.div(^^.classes("panel-body"), children)
          )
      ))
    .build

  // **************** API ****************//
  def apply(title: VdomElement,
            panelClasses: Seq[String] = Seq(),
            closeButtonCallback: Option[Callback] = None)(children: VdomNode*): VdomElement = {
    component(Props(title, panelClasses, closeButtonCallback), children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(title: VdomElement,
                           panelClasses: Seq[String],
                           closeButtonCallback: Option[Callback])
}
