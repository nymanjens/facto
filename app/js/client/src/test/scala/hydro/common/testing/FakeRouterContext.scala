package hydro.common.testing

import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.html_<^.VdomTagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

import scala.collection.mutable

class FakeRouterContext extends RouterContext {
  private val allowedPagesToNavigateTo: mutable.Set[Page] = mutable.Set()
  private var _currentPage: Page = StandardPages.UserProfile

  // **************** API implementation: Getters **************** //
  override def currentPage = _currentPage
  override def toPath(page: Page): Path = Path("/app/" + page.getClass.getSimpleName)
  override def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor] =
    <.a(^.onClick --> LogExceptionsCallback(setPage(page)))

  // **************** API implementation: Setters **************** //
  override def setPath(path: Path): Unit = ???
  override def setPage(target: Page) = {
    if (!(allowedPagesToNavigateTo contains target)) {
      throw new AssertionError(s"Not allowed to navigate to $target")
    }
  }

  // **************** Helper methods for tests **************** //
  def allowNavigationTo(page: Page): Unit = {
    allowedPagesToNavigateTo.add(page)
  }

  def setCurrentPage(page: Page): Unit = {
    _currentPage = page
  }
}
