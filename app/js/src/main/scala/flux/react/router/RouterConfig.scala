package flux.react.router

import flux.react.router.RouterConfig.Page
import flux.react.router.RouterConfig.Page.EverythingPage
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

trait RouterConfig {

  def createRouter(): Router[Page]
}

object RouterConfig {
  sealed trait Page
  object Page {
    case object EverythingPage extends Page
  }

  private[router] final class Impl(implicit reactAppModule: flux.react.app.Module) extends RouterConfig {

    override def createRouter(): Router[Page] = {
      Router(BaseUrl.until_#, routerConfig)
    }

    private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
      RouterConfigDsl[Page].buildConfig { dsl =>
        import dsl._

        // wrap/connect components to the circuit
        (staticRoute(root, EverythingPage) ~> renderR(ctl => reactAppModule.everything(30))
          | staticRoute("#everything", EverythingPage) ~> renderR(ctl => reactAppModule.everything(10))
          ).notFound(redirectToPage(EverythingPage)(Redirect.Replace))
      }.renderWith(layout)
    }

    def layout(c: RouterCtl[Page], r: Resolution[Page])(implicit reactAppModule: flux.react.app.Module) = {
      <.div(
        reactAppModule.everything(20)
      )
    }
  }
}