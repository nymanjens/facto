package flux.react.router

import japgolly.scalajs.react.extra.router.Path
import models.accounting.BalanceCheck
import models.accounting.config.{Account, MoneyReservoir, Template}
import models.accounting.money.ReferenceMoney

import scala.scalajs.js

sealed trait Page
object Page {

  abstract class HasReturnTo(private val encodedReturnTo: Option[String]) {
    def returnToPath: Path =
      Path(RouterFactory.pathPrefix + js.URIUtils.decodeURIComponent(encodedReturnTo getOrElse ""))
  }
  object HasReturnTo {
    def getCurrentEncodedPath(implicit routerContext: RouterContext): Option[String] = Some {
      val path = routerContext
        .toPath(routerContext.currentPage)
        .removePrefix(RouterFactory.pathPrefix)
        .get
        .value
      js.URIUtils.encodeURIComponent(path)
    }
  }

  case object Root extends Page

  // **************** Accounting data views **************** //
  case object Everything extends Page
  case object CashFlow extends Page
  case object Liquidation extends Page
  case object Endowments extends Page
  case object Summary extends Page
  case class Search private[router] (private[router] val encodedQuery: String) extends Page {
    def query: String = js.URIUtils.decodeURIComponent(js.URIUtils.decodeURI(encodedQuery))
  }
  object Search {
    def apply(query: String): Search = new Search(js.URIUtils.encodeURIComponent(query))
  }
  case object TemplateList extends Page

  // **************** Accounting forms - transactions **************** //
  case class NewTransactionGroup private (encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object NewTransactionGroup {
    def apply()(implicit routerContext: RouterContext): NewTransactionGroup =
      NewTransactionGroup(HasReturnTo.getCurrentEncodedPath)
  }

  case class EditTransactionGroup private (transactionGroupId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): EditTransactionGroup =
      EditTransactionGroup(transactionGroupId, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewFromTemplate private (templateCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object NewFromTemplate {
    def apply(template: Template)(implicit routerContext: RouterContext): NewFromTemplate =
      NewFromTemplate(template.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class NewForRepayment private (accountCode1: String,
                                      accountCode2: String,
                                      amountInCents: Long,
                                      encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object NewForRepayment {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney)(
        implicit routerContext: RouterContext): NewForRepayment =
      NewForRepayment(account1.code, account2.code, amount.cents, HasReturnTo.getCurrentEncodedPath)
  }

  // **************** Accounting forms - balance checks **************** //
  case class NewBalanceCheck private (reservoirCode: String, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir)(implicit routerContext: RouterContext): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, HasReturnTo.getCurrentEncodedPath)
  }

  case class EditBalanceCheck private (balanceCheckId: Long, encodedReturnTo: Option[String])
      extends HasReturnTo(encodedReturnTo)
      with Page
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck)(implicit routerContext: RouterContext): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, HasReturnTo.getCurrentEncodedPath)
  }
}
