package flux.react.router

import common.I18n
import common.money.ReferenceMoney
import japgolly.scalajs.react.extra.router.Path
import models.accounting.BalanceCheck
import models.accounting.config.{Account, MoneyReservoir, Template}

import scala.scalajs.js

sealed trait Page {
  def title(implicit i18n: I18n): String
  def iconClass: String
}
object Page {

  sealed abstract class PageBase(titleKey: String, override val iconClass: String) extends Page {
    override def title(implicit i18n: I18n) = i18n(titleKey)
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
      js.URIUtils.encodeURIComponent(path)
    }
  }

  case object Root extends Page {
    override def title(implicit i18n: I18n) = "Root"
    override def iconClass = ""
  }

  // **************** Accounting data views **************** //
  case object Everything extends PageBase("facto.everything", iconClass = "icon-list")
  case object CashFlow extends PageBase("facto.cash-flow", iconClass = "icon-money")
  case object Liquidation extends PageBase("facto.liquidation", iconClass = "icon-balance-scale")
  case object Endowments extends PageBase("facto.endowments", iconClass = "icon-crown")
  case object Summary extends PageBase("facto.summary", iconClass = "icon-table")
  case class Search private[router] (private[router] val encodedQuery: String) extends Page {
    def query: String = js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedQuery))

    override def title(implicit i18n: I18n) = i18n("facto.search-results-for", query)
    override def iconClass = "icon-list"
  }
  object Search {
    def apply(query: String): Search = new Search(js.URIUtils.encodeURIComponent(query))
  }
  case object TemplateList extends PageBase("facto.templates", iconClass = "icon-template")

  // **************** Accounting forms - transactions **************** //
  case class NewTransactionGroup private (encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.new-transaction")
    override def iconClass = "icon-new-empty"
  }
  object NewTransactionGroup {
    def apply()(implicit routerContext: RouterContext): NewTransactionGroup =
      NewTransactionGroup(HasReturnTo.getCurrentEncodedPath)
  }

  case class EditTransactionGroup private (transactionGroupId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.edit-transaction")
    override def iconClass = "icon-new-empty"
  }
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): EditTransactionGroup =
      EditTransactionGroup(transactionGroupId, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewFromTemplate private (templateCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.new-transaction")
    override def iconClass = "icon-new-empty"
  }
  object NewFromTemplate {
    def apply(template: Template)(implicit routerContext: RouterContext): NewFromTemplate =
      NewFromTemplate(template.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewForRepayment private (accountCode1: String,
                                      accountCode2: String,
                                      amountInCents: Long,
                                      encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.new-transaction")
    override def iconClass = "icon-new-empty"
  }
  object NewForRepayment {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney)(
        implicit routerContext: RouterContext): NewForRepayment =
      NewForRepayment(account1.code, account2.code, amount.cents, HasReturnTo.getCurrentEncodedPath)
  }

  // **************** Accounting forms - balance checks **************** //
  case class NewBalanceCheck private (reservoirCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.new-balance-check")
    override def iconClass = "icon-new-empty"
  }
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir)(implicit routerContext: RouterContext): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class EditBalanceCheck private (balanceCheckId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page {
    override def title(implicit i18n: I18n) = i18n("facto.edit-balance-check")
    override def iconClass = "icon-new-empty"
  }
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck)(implicit routerContext: RouterContext): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, HasReturnTo.getCurrentEncodedPath)
  }
}
