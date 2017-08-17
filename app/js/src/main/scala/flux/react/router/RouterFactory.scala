package flux.react.router

import common.LoggingUtils.logExceptions
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
        val codeString = string("[a-zA-Z0-9_-]+")

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute(root, RootPage)
            ~> redirectToPage(EverythingPage)(Redirect.Replace)

          | staticRoute("#everything", EverythingPage)
            ~> renderR(ctl => logExceptions(reactAppModule.everything(ctl)))

          | staticRoute("#cashFlow", CashFlowPage)
            ~> renderR(ctl => logExceptions(reactAppModule.cashFlow(ctl, includeHiddenReservoirs = false)))

          | staticRoute("#cashFlowWithHidden", CashFlowHiddenPage)
            ~> renderR(ctl => logExceptions(reactAppModule.cashFlow(ctl, includeHiddenReservoirs = true)))

          | staticRoute("#liquidation", LiquidationPage)
            ~> renderR(ctl => logExceptions(reactAppModule.liquidation(ctl)))

          | staticRoute("#endowments", EndowmentsPage)
            ~> renderR(ctl => logExceptions(reactAppModule.endowments(ctl)))

          | staticRoute("#newTransactionGroup", NewTransactionGroupPage)
            ~> renderR(ctl => logExceptions(reactAppModule.transactionGroupForm.forCreate(ctl)))

          | dynamicRouteCT("#editTransactionGroup" / long.caseClass[EditTransactionGroupPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, ctl))
            }

          | dynamicRouteCT("#newFromTemplate" / codeString.caseClass[NewFromTemplatePage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm.forTemplate(page.templateCode, ctl))
            }

          | dynamicRouteCT(
            "#newForRepayment" / (codeString / codeString / long).caseClass[NewForRepaymentPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm
                  .forRepayment(page.accountCode1, page.accountCode2, page.amountInCents, ctl))
            }

        // Fallback
        ).notFound(redirectToPage(EverythingPage)(Redirect.Replace))
      }
      .renderWith(layout)
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(
      implicit reactAppModule: flux.react.app.Module) = {
    reactAppModule.layout(routerCtl, resolution.page)(resolution.render())
  }
}
