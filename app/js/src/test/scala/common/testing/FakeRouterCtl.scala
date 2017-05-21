package common.testing

import flux.react.router.Page
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, RouterCtl}
import scala.collection.mutable

class FakeRouterCtl extends RouterCtl[Page] {
  private val allowedPagesToNavigateTo: mutable.Set[Page] = mutable.Set()

  override def baseUrl: BaseUrl = ???
  override def byPath: RouterCtl[Path] = ???
  override def refresh: Callback = ???
  override def pathFor(target: Page): Path = Path(target.toString)
  override def set(target: Page): Callback = Callback(
    if (!(allowedPagesToNavigateTo contains target)) {
      throw new AssertionError(s"Not allowed to navigate to $target")
    }
  )

  def allowNavigationTo(page: Page) = {
    allowedPagesToNavigateTo.add(page)
  }
}
