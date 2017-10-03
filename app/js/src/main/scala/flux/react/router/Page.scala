package flux.react.router

import japgolly.scalajs.react.extra.router.Path
import models.accounting.BalanceCheck
import models.accounting.config.{Account, MoneyReservoir, Template}
import models.accounting.money.ReferenceMoney

sealed trait Page
object Page {

  abstract class HasReturnTo(private val returnToWithoutPrefix: Option[String]) {
    def returnToPath: Path = Path(RouterFactory.pathPrefix + (returnToWithoutPrefix getOrElse ""))
  }
  object HasReturnTo {
    def getCurrentPathWithoutPrefix(implicit routerContext: RouterContext): Option[String] =
      Some(
        routerContext
          .toPath(routerContext.currentPage)
          .removePrefix(RouterFactory.pathPrefix)
          .get
          .value)
  }

  case object Root extends Page

  // **************** Accounting data views **************** //
  case object Everything extends Page
  case object CashFlow extends Page
  case object Liquidation extends Page
  case object Endowments extends Page
  case object Summary extends Page
  case class Search(query: String) extends Page
  case object TemplateList extends Page

  // **************** Accounting forms - transactions **************** //
  case class NewTransactionGroup private (returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewTransactionGroup {
    def apply()(implicit routerContext: RouterContext): NewTransactionGroup =
      NewTransactionGroup(HasReturnTo.getCurrentPathWithoutPrefix)
  }

  case class EditTransactionGroup private (transactionGroupId: Long, returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long)(implicit routerContext: RouterContext): EditTransactionGroup =
      EditTransactionGroup(transactionGroupId, HasReturnTo.getCurrentPathWithoutPrefix)
  }

  case class NewFromTemplate private (templateCode: String, returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewFromTemplate {
    def apply(template: Template)(implicit routerContext: RouterContext): NewFromTemplate =
      NewFromTemplate(template.code, HasReturnTo.getCurrentPathWithoutPrefix)
  }

  case class NewForRepayment private (accountCode1: String,
                                      accountCode2: String,
                                      amountInCents: Long,
                                      returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewForRepayment {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney)(
        implicit routerContext: RouterContext): NewForRepayment =
      NewForRepayment(account1.code, account2.code, amount.cents, HasReturnTo.getCurrentPathWithoutPrefix)
  }

  // **************** Accounting forms - balance checks **************** //
  case class NewBalanceCheck private (reservoirCode: String, returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir)(implicit routerContext: RouterContext): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, HasReturnTo.getCurrentPathWithoutPrefix)
  }

  case class EditBalanceCheck private (balanceCheckId: Long, returnToWithoutPrefix: Option[String])
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck)(implicit routerContext: RouterContext): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, HasReturnTo.getCurrentPathWithoutPrefix)
  }
}
