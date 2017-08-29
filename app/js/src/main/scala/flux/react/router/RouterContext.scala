package flux.react.router

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.{Path, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^.VdomTagOf
import org.scalajs.dom.html

/** Container for `RouterCtl` combined with the current page that provides an more tailor made API. */
trait RouterContext {

  // **************** Getters **************** //
  def currentPage: Page
  def toPath(page: Page): Path

  /**
    * Return an anchor tag that has the `href` and `onclick` attribute pre-filled to redirect the
    * browser on a click.
    */
  def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor]

  // **************** Setters **************** //
  /** Redirect the browser to the given URL path. */
  def setPath(path: Path): Unit

  /** Redirect the browser to the given page. */
  def setPage(page: Page): Unit
}

object RouterContext {
  def apply(currentPage: Page, routerCtl: RouterCtl[Page]): RouterContext =
    new RouterContext.Impl(currentPage, routerCtl)

  private final class Impl(override val currentPage: Page, routerCtl: RouterCtl[Page])
      extends RouterContext {

    // **************** Getters **************** //
    override def toPath(page: Page): Path = routerCtl.pathFor(page)
    override def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor] = routerCtl.link(page)

    // **************** Setters **************** //
    override def setPath(path: Path): Unit = routerCtl.byPath.set(path).runNow()
    override def setPage(page: Page): Unit = routerCtl.set(page).runNow()
  }

}
