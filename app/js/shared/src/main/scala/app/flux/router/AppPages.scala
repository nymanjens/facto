package app.flux.router

import app.common.accounting.ChartSpec
import app.models.accounting.BalanceCheck
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.accounting.config.MoneyReservoir
import app.models.accounting.config.Template
import hydro.common.I18n
import hydro.flux.router.Page
import hydro.flux.router.Page.PageBase
import hydro.flux.router.RouterContext
import hydro.models.access.EntityAccess

import scala.concurrent.Future
import scala.scalajs.js

object AppPages {

  sealed trait PopupEditorPage extends Page {
    def parentPage: Page
  }
  object PopupEditorPage {
    def getParentPage(routerContext: RouterContext): Page = {
      getParentPage(routerContext.currentPage)
    }
    def getParentPage(page: Page): Page = {
      page match {
        case currentPage: PopupEditorPage => currentPage.parentPage
        case currentPage                  => currentPage
      }
    }
  }

  // **************** Accounting data views **************** //
  case object Everything extends PageBase("app.everything", iconClass = "icon-list")
  case object CashFlow extends PageBase("app.cash-flow", iconClass = "icon-money")
  case object Liquidation extends PageBase("app.liquidation", iconClass = "icon-balance-scale")
  case object Endowments extends PageBase("app.endowments", iconClass = "icon-crown")
  case object Summary extends PageBase("app.summary", iconClass = "icon-table")
  case object TemplateList extends PageBase("app.templates", iconClass = "icon-template")

  // **************** Accounting forms - transactions **************** //
  case class NewTransactionGroup private (override val parentPage: Page) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroup {
    def apply()(implicit routerContext: RouterContext): NewTransactionGroup =
      NewTransactionGroup(parentPage = PopupEditorPage.getParentPage(routerContext))
  }

  case class EditTransactionGroup private (
      transactionGroupId: Long,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.edit-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): EditTransactionGroup = {
      EditTransactionGroup(
        transactionGroupId = transactionGroupId,
        parentPage = PopupEditorPage.getParentPage(routerContext),
      )
    }
  }

  case class NewTransactionGroupFromCopy private (
      transactionGroupId: Long,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroupFromCopy {
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): NewTransactionGroupFromCopy =
      NewTransactionGroupFromCopy(
        transactionGroupId,
        parentPage = PopupEditorPage.getParentPage(routerContext),
      )
  }

  case class NewTransactionGroupFromReservoir private (
      reservoirCode: String,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroupFromReservoir {
    def apply(reservoir: MoneyReservoir)(implicit
        routerContext: RouterContext
    ): NewTransactionGroupFromReservoir =
      NewTransactionGroupFromReservoir(
        reservoir.code,
        parentPage = PopupEditorPage.getParentPage(routerContext),
      )
  }

  case class NewFromTemplate private (
      templateCode: String,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewFromTemplate {
    def apply(template: Template)(implicit routerContext: RouterContext): NewFromTemplate =
      NewFromTemplate(template.code, parentPage = PopupEditorPage.getParentPage(routerContext))
  }

  case class NewForRepayment private (
      accountCode1: String,
      accountCode2: String,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewForRepayment {
    def apply(account1: Account, account2: Account)(implicit routerContext: RouterContext): NewForRepayment =
      NewForRepayment(account1.code, account2.code, parentPage = PopupEditorPage.getParentPage(routerContext))
  }
  case class NewForLiquidationSimplification private (
      override val parentPage: Page
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.simplify-liquidation"))
    override def iconClass = "icon-new-empty"
  }
  object NewForLiquidationSimplification {
    def apply()(implicit routerContext: RouterContext): NewForLiquidationSimplification =
      NewForLiquidationSimplification(parentPage = PopupEditorPage.getParentPage(routerContext))
  }

  // **************** Accounting forms - balance checks **************** //
  case class NewBalanceCheck private (
      reservoirCode: String,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-balance-check"))
    override def iconClass = "icon-new-empty"
  }
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir)(implicit routerContext: RouterContext): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, parentPage = PopupEditorPage.getParentPage(routerContext))
  }

  case class EditBalanceCheck private (
      balanceCheckId: Long,
      override val parentPage: Page,
  ) extends PopupEditorPage {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.edit-balance-check"))
    override def iconClass = "icon-new-empty"
  }
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck)(implicit routerContext: RouterContext): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, parentPage = PopupEditorPage.getParentPage(routerContext))
  }

  // **************** Chart **************** //
  case class Chart private (encodedChartSpecs: String)
      extends PageBase("app.chart", iconClass = "fa fa-bar-chart-o") {
    def chartSpec: ChartSpec = {
      ChartSpec.parseStringified(js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedChartSpecs)))
    }
  }
  object Chart {

    def fromChartSpec(chartSpec: ChartSpec): Chart = {
      new Chart(
        js.URIUtils.encodeURIComponent(chartSpec.stringify)
      )
    }

    def firstPredefined(implicit accountingConfig: Config): Chart = {
      Chart.fromChartSpec(
        accountingConfig.predefinedCharts.headOption.map(_.chartSpec) getOrElse ChartSpec
          .singleEmptyLine(correctForInflation = false)
      )
    }
  }
}
