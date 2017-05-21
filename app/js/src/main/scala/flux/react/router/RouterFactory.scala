package flux.react.router

import flux.react.router.Page._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._

private[router] final class RouterFactory(implicit reactAppModule: flux.react.app.Module) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until_#, routerConfig)
  }

  private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Page].buildConfig { dsl =>
      import dsl._

      // wrap/connect components to the circuit
      (staticRoute(root, EverythingPage) ~> renderR(ctl => reactAppModule.everything(ctl))
        | staticRoute("#everything", EverythingPage2) ~> renderR(ctl => <.div(reactAppModule.everything(ctl), reactAppModule.everything(ctl), reactAppModule.everything(ctl)))
        | staticRoute("#newTransactionGroup", NewTransactionGroupPage) ~> renderR(ctl => reactAppModule.transactionGroupForm(ctl))
        | dynamicRouteCT("#editTransactionGroup" / long.caseClass[EditTransactionGroupPage]) ~> renderR(ctl => reactAppModule.transactionGroupForm(ctl))
        | staticRoute("#test", TestPage) ~> renderR(ctl => reactAppModule.menu(TestPage, ctl))
        ).notFound(redirectToPage(EverythingPage)(Redirect.Replace))
    }.renderWith(layout)
  }

  private def layout(c: RouterCtl[Page], r: Resolution[Page])(implicit reactAppModule: flux.react.app.Module) = {
    <.div(
      reactAppModule.menu(r.page, c),
      r.render()
    )
  }
}
