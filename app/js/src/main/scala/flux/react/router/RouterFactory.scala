package flux.react.router

import common.LoggingUtils.logExceptions
import flux.react.router.Page._
import japgolly.scalajs.react.extra.router.StaticDsl.{DynamicRouteB, RouteB, StaticRouteB}
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

import scala.reflect.ClassTag

private[router] final class RouterFactory(implicit reactAppModule: flux.react.app.Module) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until(RouterFactory.pathPrefix), routerConfig)
  }

  private def routerConfig(implicit reactAppModule: flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        import dsl._
        val codeString = string("[a-zA-Z0-9_-]+")
        val returnToPath = "?returnto=" / string(".+")

        def staticRuleFromPage(page: Page, renderer: RouterCtl[Page] => VdomElement): dsl.Rule = {
          val path = RouterFactory.pathPrefix + page.getClass.getSimpleName.stripSuffix("Page").toLowerCase
          staticRoute(path, page) ~> renderR(ctl => logExceptions(renderer(ctl)))
        }
        def dynamicRuleFromPage[P <: Page](dynamicPart: String => RouteB[P])(
            renderer: (P, RouterCtl[Page]) => VdomElement)(implicit pageClass: ClassTag[P]): dsl.Rule = {
          val staticPathPart = RouterFactory.pathPrefix + pageClass.runtimeClass.getSimpleName
            .stripSuffix("Page")
            .toLowerCase
          val path = dynamicPart(staticPathPart)
          dynamicRouteCT(path) ~> dynRenderR {
            case (page, ctl) => logExceptions(renderer(page, ctl))
          }
        }

        // wrap/connect components to the circuit
        (emptyRule

          | staticRoute("/app/", RootPage) ~> redirectToPage(CashFlowPage)(Redirect.Replace)

          | staticRuleFromPage(EverythingPage, reactAppModule.everything.apply _)

          | staticRuleFromPage(CashFlowPage, reactAppModule.cashFlow.apply _)

          | staticRuleFromPage(LiquidationPage, reactAppModule.liquidation.apply _)

          | staticRuleFromPage(EndowmentsPage, reactAppModule.endowments.apply _)

          | staticRuleFromPage(NewTransactionGroupPage, reactAppModule.transactionGroupForm.forCreate _)

          | dynamicRuleFromPage(_ / long.caseClass[EditTransactionGroupPage]) { (page, ctl) =>
            reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, ctl)
          }

          | dynamicRuleFromPage(_ / codeString.caseClass[NewFromTemplatePage]) { (page, ctl) =>
            reactAppModule.transactionGroupForm.forTemplate(page.templateCode, ctl)
          }

          | dynamicRuleFromPage(_ / (codeString / codeString / long).caseClass[NewForRepaymentPage]) {
            (page, ctl) =>
              reactAppModule.transactionGroupForm
                .forRepayment(page.accountCode1, page.accountCode2, page.amountInCents, ctl)
          }

          | dynamicRuleFromPage(_ / (codeString / returnToPath).caseClass[NewBalanceCheckPage]) {
            (page, ctl) =>
              reactAppModule.balanceCheckForm.forCreate(page.reservoirCode, page.returnToPath, ctl)
          }

          | dynamicRuleFromPage(_ / (long / returnToPath).caseClass[EditBalanceCheckPage]) { (page, ctl) =>
            reactAppModule.balanceCheckForm.forEdit(page.balanceCheckId, page.returnToPath, ctl)
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
private[router] object RouterFactory {
  val pathPrefix = "/app/"
}
