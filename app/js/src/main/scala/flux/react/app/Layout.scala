package flux.react.app

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.router.Page
import flux.stores.GlobalMessagesStore
import japgolly.scalajs.react.extra.router.{Resolution, RouterCtl}

import scala.collection.immutable.Seq

final class Layout(implicit globalMessages: GlobalMessages, menu: Menu) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderPC(
      (_, props, children) =>
        <.div(
          menu(props.page, props.routerCtl),
          globalMessages(),
          children
      ))
    .build

  // **************** API ****************//
  def apply(routerCtl: RouterCtl[Page], page: Page)(children: ReactNode*): ReactElement = {
    component(Props(routerCtl, page), children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(routerCtl: RouterCtl[Page], page: Page)
}
