package flux.react.app

import flux.react.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class Layout(implicit globalMessages: GlobalMessages, menu: Menu) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC(
      (_, props, children) =>
        <.div(
          menu(props.router),
          globalMessages(),
          children
      ))
    .build

  // **************** API ****************//
  def apply(router: RouterContext)(children: VdomNode*): VdomElement = {
    component(Props(router))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
