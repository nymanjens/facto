package flux.react.router

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.{Path, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^.VdomTagOf
import org.scalajs.dom.html

/** Container for `RouterCtl` combined with the current page that provides an more tailor made API. */
final class RouterContext private (_currentPage: Page, routerCtl: RouterCtl[Page]) {

  def currentPage: Page = _currentPage
  def setPath(path: Path): Unit = routerCtl.byPath.set(path).runNow()
  def setPage(page: Page): Unit = routerCtl.set(page).runNow()
  def toHref(page: Page): String = routerCtl.urlFor(page).value
  def toPath(page: Page): Path = routerCtl.pathFor(page)
  def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor] = routerCtl.link(page)
}

object RouterContext {
  def apply(currentPage: Page, routerCtl: RouterCtl[Page]): RouterContext =
    new RouterContext(currentPage, routerCtl)
}
