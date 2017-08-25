package flux.react.router

import common.LoggingUtils.logExceptions
import flux.react.router.Page._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

private[router] final class RouterFactory(implicit reactAppModule: flux.react.app.Module) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until("/app/"), routerConfig)
  }

  private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        import dsl._
        val codeString = string("[a-zA-Z0-9_-]+")

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute("/app/", RootPage)
            ~> redirectToPage(CashFlowPage)(Redirect.Replace)

          | staticRoute("/app/everything", EverythingPage)
            ~> renderR(ctl => logExceptions(reactAppModule.everything(ctl)))

          | staticRoute("/app/cashFlow", CashFlowPage)
            ~> renderR(ctl => logExceptions(reactAppModule.cashFlow(ctl)))

          | staticRoute("/app/liquidation", LiquidationPage)
            ~> renderR(ctl => logExceptions(reactAppModule.liquidation(ctl)))

          | staticRoute("/app/endowments", EndowmentsPage)
            ~> renderR(ctl => logExceptions(reactAppModule.endowments(ctl)))

          | staticRoute("/app/newTransactionGroup", NewTransactionGroupPage)
            ~> renderR(ctl => logExceptions(reactAppModule.transactionGroupForm.forCreate(ctl)))

          | dynamicRouteCT("/app/editTransactionGroup" / long.caseClass[EditTransactionGroupPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, ctl))
            }

          | dynamicRouteCT("/app/newFromTemplate" / codeString.caseClass[NewFromTemplatePage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm.forTemplate(page.templateCode, ctl))
            }

          | dynamicRouteCT(
            "/app/newForRepayment" / (codeString / codeString / long).caseClass[NewForRepaymentPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.transactionGroupForm
                  .forRepayment(page.accountCode1, page.accountCode2, page.amountInCents, ctl))
            }

          | dynamicRouteCT("/app/newBalanceCheck" / codeString.caseClass[NewBalanceCheckPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.balanceCheckForm.forCreate(page.reservoirCode, ctl))
            }

          | dynamicRouteCT("/app/editBalanceCheck" / long.caseClass[EditBalanceCheckPage])
            ~> dynRenderR {
              case (page, ctl) =>
                logExceptions(reactAppModule.balanceCheckForm.forEdit(page.balanceCheckId, ctl))
            }

        // Fallback
        ).notFound(redirectToPage(CashFlowPage)(Redirect.Replace))
      }
      .renderWith(layout)
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(
      implicit reactAppModule: flux.react.app.Module) = {
    reactAppModule.layout(routerCtl, resolution.page)(resolution.render())
  }
}
