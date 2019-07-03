package hydro.flux.react.uielements

import hydro.common.I18n
import hydro.flux.router.Page
import hydro.models.access.EntityAccess
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

final class PageHeader(implicit i18n: I18n, entityAccess: EntityAccess) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderPC { (_, props, children) =>
      <.h1(
        ^.className := "page-header",
        <.i(^.className := props.iconClass),
        " ",
        props.title,
        " ",
        children
      )
    }
    .build
  private val waitForFuture = new WaitForFuture[String]

  // **************** API ****************//
  def apply(page: Page, title: String = null): VdomElement = {
    withExtension(page, title)()
  }

  def withExtension(page: Page, title: String = null)(children: VdomNode*): VdomElement = {
    def newComponent(title: String): VdomElement =
      component(Props(title = title, iconClass = page.iconClass))(children: _*)
    if (title != null) {
      newComponent(title = title)
    } else {
      waitForFuture(futureInput = page.title, waitingElement = newComponent(title = "")) { titleFromPage =>
        newComponent(title = titleFromPage)
      }
    }
  }

  // **************** Private inner types ****************//
  private case class Props(title: String, iconClass: String)
}
