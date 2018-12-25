package flux.router

import common.I18n
import japgolly.scalajs.react.extra.router.Path
import models.access.EntityAccess
import models.accounting.BalanceCheck
import models.accounting.config.Account
import models.accounting.config.MoneyReservoir
import models.accounting.config.Template

import scala.concurrent.Future
import scala.scalajs.js

sealed trait Page {
  def title(implicit i18n: I18n, entityAccess: EntityAccess): Future[String]
  def iconClass: String
}
object Page {

  sealed abstract class PageBase(titleKey: String, override val iconClass: String) extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful(titleSync)
    def titleSync(implicit i18n: I18n) = i18n(titleKey)
  }

  sealed abstract class HasReturnTo(private val encodedReturnTo: Option[String]) {
    def returnToPath: Path =
      Path(RouterFactory.pathPrefix + js.URIUtils.decodeURIComponent(encodedReturnTo getOrElse ""))
  }
  private object HasReturnTo {
    def getCurrentEncodedPath(implicit routerContext: RouterContext): Option[String] = Some {
      val path = routerContext
        .toPath(routerContext.currentPage)
        .removePrefix(RouterFactory.pathPrefix)
        .get
        .value
      js.URIUtils.encodeURIComponent(
        // Decode path first because routerContext.toPath() seems to produce unnecessarily and
        // inconsistently escaped strings
        js.URIUtils.decodeURIComponent(path))
    }
  }

  case object Root extends Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) = Future.successful("Root")
    override def iconClass = ""
  }

  // **************** User management views **************** //
  case object UserProfile extends PageBase("app.user-profile", iconClass = "fa fa-user fa-fw")
  case object UserAdministration extends PageBase("app.user-administration", iconClass = "fa fa-cogs fa-fw")

  // **************** Accounting data views **************** //
  case object Everything extends PageBase("app.everything", iconClass = "icon-list")
  case object CashFlow extends PageBase("app.cash-flow", iconClass = "icon-money")
  case object Liquidation extends PageBase("app.liquidation", iconClass = "icon-balance-scale")
  case object Endowments extends PageBase("app.endowments", iconClass = "icon-crown")
  case object Summary extends PageBase("app.summary", iconClass = "icon-table")
  case class Search private[router] (private[router] val encodedQuery: String) extends Page {
    def query: String = js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedQuery))

    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.search-results-for", query))
    override def iconClass = "icon-list"
  }
  object Search {
    def apply(query: String): Search = new Search(js.URIUtils.encodeURIComponent(query))
  }
  case object TemplateList extends PageBase("app.templates", iconClass = "icon-template")

  // **************** Accounting forms - transactions **************** //
  case class NewTransactionGroup private (encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroup {
    def apply()(implicit routerContext: RouterContext): NewTransactionGroup =
      NewTransactionGroup(HasReturnTo.getCurrentEncodedPath)
  }

  case class EditTransactionGroup private (transactionGroupId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.edit-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): EditTransactionGroup =
      EditTransactionGroup(transactionGroupId, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewTransactionGroupFromReservoir private (reservoirCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroupFromReservoir {
    def apply(reservoir: MoneyReservoir)(
        implicit routerContext: RouterContext): NewTransactionGroupFromReservoir =
      NewTransactionGroupFromReservoir(reservoir.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewFromTemplate private (templateCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewFromTemplate {
    def apply(template: Template)(implicit routerContext: RouterContext): NewFromTemplate =
      NewFromTemplate(template.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewForRepayment private (accountCode1: String,
                                      accountCode2: String,
                                      encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-transaction"))
    override def iconClass = "icon-new-empty"
  }
  object NewForRepayment {
    def apply(account1: Account, account2: Account)(implicit routerContext: RouterContext): NewForRepayment =
      NewForRepayment(account1.code, account2.code, HasReturnTo.getCurrentEncodedPath)
  }
  case class NewForLiquidationSimplification private (encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.simplify-liquidation"))
    override def iconClass = "icon-new-empty"
  }
  object NewForLiquidationSimplification {
    def apply()(implicit routerContext: RouterContext): NewForLiquidationSimplification =
      NewForLiquidationSimplification(HasReturnTo.getCurrentEncodedPath)
  }

  // **************** Accounting forms - balance checks **************** //
  case class NewBalanceCheck private (reservoirCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.new-balance-check"))
    override def iconClass = "icon-new-empty"
  }
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir)(implicit routerContext: RouterContext): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class EditBalanceCheck private (balanceCheckId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n, entityAccess: EntityAccess) =
      Future.successful(i18n("app.edit-balance-check"))
    override def iconClass = "icon-new-empty"
  }
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck)(implicit routerContext: RouterContext): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, HasReturnTo.getCurrentEncodedPath)
  }
}
