package common.testing

import flux.react.router.{Page, RouterContext}
import japgolly.scalajs.react.Callback
import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^.VdomTagOf
import org.scalajs.dom.html
import org.scalajs.dom.html.Anchor
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.mutable

class FakeRouterContext extends RouterContext {
  private val allowedPagesToNavigateTo: mutable.Set[Page] = mutable.Set()

  // **************** Getters **************** //
  override def currentPage = ???
  override def toPath(page: Page): Path = ???
  override def anchorWithHrefTo(page: Page): VdomTagOf[html.Anchor] =
    <.a(^.onClick --> LogExceptionsCallback(setPage(page)))

  // **************** Setters **************** //
  override def setPath(path: Path): Unit = ???
  override def setPage(target: Page) = {
    if (!(allowedPagesToNavigateTo contains target)) {
      throw new AssertionError(s"Not allowed to navigate to $target")
    }
  }

  def allowNavigationTo(page: Page) = {
    allowedPagesToNavigateTo.add(page)
  }
}
