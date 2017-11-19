package flux.react.uielements

import common.I18n
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom.html_<^._
import flux.react.ReactVdomUtils.{<<, ^^}
import flux.react.router.Page

import scala.collection.immutable.Seq

object PageHeader {
  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      <.h1(
        ^.className := "page-header",
        <.i(^.className := props.page.iconClass),
        " ",
        props.page.title(props.i18n),
        children
      )
    }
    .build

  // **************** API ****************//
  def apply(page: Page)(implicit i18n: I18n): VdomElement = {
    component(Props(page))()
  }
  def withExtension(page: Page)(children: VdomNode*)(implicit i18n: I18n): VdomElement = {
    component(Props(page))(children: _*)
  }

  // **************** Private inner types ****************//
  private case class Props(page: Page)(implicit val i18n: I18n)
}
