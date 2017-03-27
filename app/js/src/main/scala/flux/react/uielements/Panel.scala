package flux.react.uielements

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{^^, <<}

import scala.collection.immutable.Seq

object Panel {
  private case class Props(title: String, panelClasses: Seq[String])
  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderPC((_, props, children) =>
      <.div(^^.classes("row" +: props.panelClasses),
        <.div(^^.classes("col-lg-12"),
          <.div(^^.classes("panel panel-default"),
            <.div(^^.classes("panel-heading"),
              props.title
            ),
            <.div(^^.classes("panel-body"),
              children
            )
          )
        )
      )
    ).build

  def apply(title: String, panelClasses: Seq[String] = Seq())(children: ReactNode*): ReactElement = {
    component(Props(title, panelClasses), children: _*)
  }
}
