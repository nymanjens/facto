package flux.react.router

import flux.react.router.Page._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

private[router] final class RouterFactory(implicit reactAppModule: flux.react.app.Module) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until_#, routerConfig)
  }

  private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        import dsl._

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute(root, EverythingPage)
            ~> renderR(ctl => reactAppModule.everything(ctl))

          | staticRoute("#endowments", EndowmentsPage)
            ~> renderR(ctl => reactAppModule.endowments(ctl))

          | staticRoute("#everything", EverythingPage2)
            ~> renderR(
              ctl =>
                <.div(
                  reactAppModule.everything(ctl),
                  reactAppModule.everything(ctl),
                  reactAppModule.everything(ctl)))

          | staticRoute("#newTransactionGroup", NewTransactionGroupPage)
            ~> renderR(ctl => reactAppModule.transactionGroupForm.forCreate(ctl))

          | dynamicRouteCT("#editTransactionGroup" / long.caseClass[EditTransactionGroupPage])
            ~> dynRenderR {
              case (page, ctl) => reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, ctl)
            }).notFound(redirectToPage(EverythingPage)(Redirect.Replace))
      }
      .renderWith(layout)
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(
      implicit reactAppModule: flux.react.app.Module) = {
    reactAppModule.layout(routerCtl, resolution.page)(resolution.render())
  }
}
