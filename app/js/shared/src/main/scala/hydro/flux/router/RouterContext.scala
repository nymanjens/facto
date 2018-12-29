package hydro.flux.router

import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.CallbackOption
import japgolly.scalajs.react.ReactMouseEvent
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

import scala.scalajs.js

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
  def apply(currentPage: Page, routerCtl: RouterCtl[Page])(implicit dispatcher: Dispatcher): RouterContext =
    new RouterContext.Impl(currentPage, routerCtl)

  private final class Impl(override val currentPage: Page, routerCtl: RouterCtl[Page])(
      implicit dispatcher: Dispatcher)
      extends RouterContext {

    // **************** Getters **************** //
    override def toPath(page: Page): Path = routerCtl.pathFor(page)
    override def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor] = {
      def go(e: ReactMouseEvent): Callback =
        CallbackOption.unless(ReactMouseEvent targetsNewTab_? e) >>
          CallbackOption.asEventDefault(e, Callback { setPage(page) })
      <.a(^.href := routerCtl.urlFor(page).value, ^.onClick ==> go)
    }

    // **************** Setters **************** //
    override def setPath(path: Path): Unit = {
      startRender(routerCtl.byPath.set(path))
    }
    override def setPage(page: Page): Unit = {
      startRender(routerCtl.set(page))
    }

    private def startRender(setAction: => Callback): Unit = {
      dispatcher.dispatch(StandardActions.SetPageLoadingState(isLoading = true))
      js.timers.setTimeout(0)(setAction.runNow())
    }
  }
}
