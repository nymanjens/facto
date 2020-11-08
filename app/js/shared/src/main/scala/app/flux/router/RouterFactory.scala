package app.flux.router

import app.flux.router.AppPages.PopupEditorPage
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.action.Dispatcher
import hydro.flux.action.StandardActions
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.router.Page
import hydro.flux.router.RouterContext
import hydro.flux.router.StandardPages
import hydro.models.access.EntityAccess
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.Callback
import org.scalajs.dom

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.reflect.ClassTag
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[router] final class RouterFactory(implicit
    reactAppModule: app.flux.react.app.Module,
    dispatcher: Dispatcher,
    i18n: I18n,
    entityAccess: EntityAccess,
) {

  def createRouter(): Router[Page] = {
    Router(BaseUrl.until(RouterFactory.pathPrefix), routerConfig)
  }

  private def routerConfig(implicit reactAppModule: app.flux.react.app.Module) = {
    RouterConfigDsl[Page]
      .buildConfig { dsl =>
        implicit val implicitDsl: RouterConfigDsl[Page] = dsl
        import dsl._
        val codeString: RouteB[String] = string("[a-zA-Z0-9_-]+")
        val query: RouteB[String] = "?q=" ~ string(".+")

        val factoRouterConfig = FactoRouterConfig(
          parentRules = Seq(
            ParentRule.static(StandardPages.UserProfile, reactAppModule.userProfile.apply),
            ParentRule.static(StandardPages.UserAdministration, reactAppModule.userAdministration.apply),
            ParentRule.static(StandardPages.DatabaseExplorer, reactAppModule.databaseExplorer.apply),
            ParentRule.static(AppPages.Everything, reactAppModule.everything.apply),
            ParentRule.static(AppPages.CashFlow, reactAppModule.cashFlow.apply),
            ParentRule.static(AppPages.Liquidation, reactAppModule.liquidation.apply),
            ParentRule.static(AppPages.Endowments, reactAppModule.endowments.apply),
            ParentRule.static(AppPages.Summary, reactAppModule.summary.apply),
            ParentRule.dynamic(query.caseClass[StandardPages.Search]) { (page, ctl) =>
              reactAppModule.searchResults(page.query, ctl)
            },
            ParentRule.static(AppPages.TemplateList, reactAppModule.templateList.apply),
            ParentRule.dynamic(query.caseClass[AppPages.Chart]) { (page, ctl) =>
              reactAppModule.chart(page.chartSpec, ctl)
            },
          ),
          popupRules = Seq(
            PopupRule.static(_.caseClass[AppPages.NewTransactionGroup]) { (page, ctl) =>
              reactAppModule.transactionGroupForm.forCreate(ctl)
            },
            PopupRule.dynamic(suffix => (long ~ suffix).caseClass[AppPages.EditTransactionGroup]) {
              (page, ctl) =>
                reactAppModule.transactionGroupForm.forEdit(page.transactionGroupId, ctl)
            },
            PopupRule.dynamic(suffix => (long ~ suffix).caseClass[AppPages.NewTransactionGroupFromCopy]) {
              (page, ctl) =>
                reactAppModule.transactionGroupForm.forCreateFromCopy(page.transactionGroupId, ctl)
            },
            PopupRule.dynamic(suffix =>
              (codeString ~ suffix).caseClass[AppPages.NewTransactionGroupFromReservoir]
            ) { (page, ctl) =>
              reactAppModule.transactionGroupForm.forReservoir(page.reservoirCode, ctl)
            },
            PopupRule.dynamic(suffix => (codeString ~ suffix).caseClass[AppPages.NewFromTemplate]) {
              (page, ctl) =>
                reactAppModule.transactionGroupForm.forTemplate(page.templateCode, ctl)
            },
            PopupRule.dynamic(suffix =>
              ((codeString / codeString) ~ suffix).caseClass[AppPages.NewForRepayment]
            ) { (page, ctl) =>
              reactAppModule.transactionGroupForm.forRepayment(page.accountCode1, page.accountCode2, ctl)
            },
            PopupRule.static(_.caseClass[AppPages.NewForLiquidationSimplification]) { (page, ctl) =>
              reactAppModule.transactionGroupForm.forLiquidationSimplification(ctl)
            },
            PopupRule.dynamic(suffix => (codeString ~ suffix).caseClass[AppPages.NewBalanceCheck]) {
              (page, ctl) =>
                reactAppModule.balanceCheckForm.forCreate(page.reservoirCode, ctl)
            },
            PopupRule.dynamic(suffix => (long ~ suffix).caseClass[AppPages.EditBalanceCheck]) { (page, ctl) =>
              reactAppModule.balanceCheckForm.forEdit(page.balanceCheckId, ctl)
            },
          ),
        )

        // wrap/connect components to the circuit
        (
          emptyRule
            | staticRoute(RouterFactory.pathPrefix, StandardPages.Root)
            ~> redirectToPage(AppPages.CashFlow)(Redirect.Replace)

            | factoRouterConfig.parentRules.map(_.rule).reduceLeft(_ | _)

            | (
              for {
                parentRule <- factoRouterConfig.parentRules
                popupRule <- factoRouterConfig.popupRules
              } yield popupRule.ruleFromParent(parentRule)
            ).reduceLeft(_ | _)

          // Fallback
        ).notFound(redirectToPage(AppPages.CashFlow)(Redirect.Replace))
      }
      .renderWith(layout)
      // Clear post render for popups because the default scrolls to the top
      .setPostRender((maybePreviuosPage, currentPage) => {
        maybePreviuosPage match {
          case Some(previousPage)
              if PopupEditorPage.getParentPage(previousPage) == PopupEditorPage.getParentPage(currentPage) =>
            Callback.empty
          case _ => RouterConfig.defaultPostRenderFn(maybePreviuosPage, currentPage)
        }
      })
      // add additional post-render that should happen every time
      .onPostRender((prev, cur) =>
        LogExceptionsCallback(dispatcher.dispatch(StandardActions.SetPageLoadingState(isLoading = false)))
      )
      .onPostRender((_, page) =>
        LogExceptionsCallback(async {
          val title = await(page.title)
          dom.document.title = s"$title | Facto"
        })
      )
  }

  private def layout(routerCtl: RouterCtl[Page], resolution: Resolution[Page])(implicit
      reactAppModule: app.flux.react.app.Module
  ) = {
    reactAppModule.layout(RouterContext(resolution.page, routerCtl))(
      <.div(resolution.render())
    )
  }

  private def renderPageMaybeWithPopup(
      page: Page,
      parent: VdomElement,
      maybePopup: Option[VdomElement],
  ): VdomElement = {
    <.span(
      ^.key := "page-maybe-with-popup",
      <.span(^.key := s"parent-${PopupEditorPage.getParentPage(page)}", parent),
      <<.ifDefined(maybePopup) { popup =>
        <.span(^.key := s"popup-$page", popup)
      },
    )
  }

  private case class FactoRouterConfig(
      parentRules: Seq[ParentRule.any],
      popupRules: Seq[PopupRule.any],
  )

  private case class ParentRule[P <: Page](
      routeWithoutPrefix: RouteB[P],
      private val renderer: (P, RouterContext) => VdomElement,
      rule: StaticDsl.Rule[Page],
  )(implicit
      val classTag: ClassTag[P]
  ) {
    def render(page: Page, context: RouterContext): VdomElement = {
      renderer(page.asInstanceOf[P], context)
    }
  }
  private object ParentRule {
    type any = ParentRule[_ <: Page]

    def static[P <: Page](page: P, renderer: RouterContext => VdomElement)(implicit
        pageClass: ClassTag[P],
        dsl: RouterConfigDsl[Page],
    ): ParentRule[P] = {
      import dsl._

      val routeWithoutPrefix: RouteB[Unit] = page.getClass.getSimpleName.toLowerCase

      ParentRule[P](
        routeWithoutPrefix = routeWithoutPrefix const page,
        renderer = (p, context) => renderer(context),
        rule = staticRoute(RouterFactory.pathPrefix ~ routeWithoutPrefix, page) ~> renderR(ctl =>
          logExceptions(
            renderPageMaybeWithPopup(page, parent = renderer(RouterContext(page, ctl)), maybePopup = None)
          )
        ),
      )
    }

    def dynamic[P <: Page](
        dynamicPartOfRoute: RouteB[P]
    )(
        renderer: (P, RouterContext) => VdomElement
    )(implicit
        pageClass: ClassTag[P],
        dsl: RouterConfigDsl[Page],
    ): ParentRule[P] = {
      import dsl._

      val routeWithoutPrefix = pageClass.runtimeClass.getSimpleName.toLowerCase / dynamicPartOfRoute

      ParentRule[P](
        routeWithoutPrefix = routeWithoutPrefix,
        renderer = renderer,
        rule =
          dynamicRouteCT[P](RouterFactory.pathPrefix ~ routeWithoutPrefix) ~> dynRenderR { case (page, ctl) =>
            logExceptions(
              renderPageMaybeWithPopup(
                page,
                parent = renderer(page, RouterContext(page, ctl)),
                maybePopup = None,
              )
            )
          },
      )
    }
  }

  private case class PopupRule[P <: PopupEditorPage](
      ruleFromParent: ParentRule.any => StaticDsl.Rule[Page]
  )
  private object PopupRule {
    type any = PopupRule[_ <: PopupEditorPage]

    def static[P <: PopupEditorPage](
        parentToPopupPage: RouteB[Page] => RouteB[P]
    )(
        popupRenderer: (P, RouterContext) => VdomElement
    )(implicit
        pageClass: ClassTag[P],
        dsl: RouterConfigDsl[Page],
    ): PopupRule[P] = {
      import dsl._

      val pageClassName = pageClass.runtimeClass.getSimpleName.toLowerCase

      PopupRule(
        ruleFromParent = parentRule => {
          val route: RouteB[P] =
            RouterFactory.pathPrefix ~ "@" ~ pageClassName ~ "@" /
              parentToPopupPage(parentRule.routeWithoutPrefix.asInstanceOf[RouteB[Page]])

          dynamicRouteForPopup(route, parentRule, popupRenderer)
        }
      )
    }

    def dynamic[P <: PopupEditorPage](
        prependDynamicPart: RouteB[Page] => RouteB[P]
    )(
        popupRenderer: (P, RouterContext) => VdomElement
    )(implicit
        pageClass: ClassTag[P],
        dsl: RouterConfigDsl[Page],
    ): PopupRule[P] = {
      import dsl._

      val pageClassName = pageClass.runtimeClass.getSimpleName.toLowerCase

      PopupRule(
        ruleFromParent = parentRule => {
          val route: RouteB[P] =
            RouterFactory.pathPrefix ~ "@" ~ pageClassName /
              prependDynamicPart("@" / parentRule.routeWithoutPrefix.asInstanceOf[RouteB[Page]])

          dynamicRouteForPopup(route, parentRule, popupRenderer)
        }
      )
    }

    private def dynamicRouteForPopup[P <: PopupEditorPage](
        route: RouteB[P],
        parentRule: ParentRule.any,
        popupRenderer: (P, RouterContext) => VdomElement,
    )(implicit
        dsl: RouterConfigDsl[Page],
        pageClass: ClassTag[P],
    ): StaticDsl.Rule[Page] = {
      import dsl._

      dynamicRoute[P](route) {
        case p: P if parentRule.classTag.runtimeClass == p.parentPage.getClass => p
      } ~> dynRenderR { case (page, ctl) =>
        logExceptions {
          renderPageMaybeWithPopup(
            page = page,
            parent = parentRule.render(page.parentPage, RouterContext(page.parentPage, ctl)),
            maybePopup = Some(
              <.div(
                ^.className := "popup-editor",
                <.div(
                  ^.className := "close-button-container",
                  Bootstrap.Button(
                    variant = Variant.default,
                    size = Size.sm,
                    tag = RouterContext(page, ctl).anchorWithHrefTo(page.parentPage),
                  )(
                    Bootstrap.FontAwesomeIcon("times", fixedWidth = true)
                  ),
                ),
                popupRenderer(page, RouterContext(page, ctl)),
              )
            ),
          )
        }
      }
    }
  }
}
private[router] object RouterFactory {
  val pathPrefix = "/app/"
}
