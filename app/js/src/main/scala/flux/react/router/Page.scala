package flux.react.router

import japgolly.scalajs.react.extra.router.Path
import models.accounting.{BalanceCheck, TransactionGroup}
import models.accounting.config.{Account, MoneyReservoir, Template}
import models.accounting.money.ReferenceMoney
import org.scalajs.dom

sealed trait Page
object Page {

  abstract class HasReturnTo(private val returnToWithoutPrefix: String) {
    def returnToPath: Path = Path(RouterFactory.pathPrefix + returnToWithoutPrefix)
  }
  object HasReturnTo {
    def getDomPathWithoutPrefix: String = dom.window.location.pathname.stripPrefix(RouterFactory.pathPrefix)
  }

  case object Root extends Page

  // Accounting data views
  case object Everything extends Page
  case object CashFlow extends Page
  case object Liquidation extends Page
  case object Endowments extends Page

  // Accounting forms - transactions
  case class NewTransactionGroup private (returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewTransactionGroup {
    def apply(): NewTransactionGroup = NewTransactionGroup(HasReturnTo.getDomPathWithoutPrefix)
  }
  case class EditTransactionGroup private (transactionGroupId: Long, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditTransactionGroup {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long): EditTransactionGroup =
      EditTransactionGroup(transactionGroupId, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewFromTemplate private (templateCode: String, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewFromTemplate {
    def apply(template: Template): NewFromTemplate =
      NewFromTemplate(template.code, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewForRepayment private (accountCode1: String,
                                      accountCode2: String,
                                      amountInCents: Long,
                                      returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewForRepayment {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney): NewForRepayment =
      NewForRepayment(account1.code, account2.code, amount.cents, HasReturnTo.getDomPathWithoutPrefix)
  }

  // Accounting forms - balance checks
  case class NewBalanceCheck private (reservoirCode: String, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewBalanceCheck {
    def apply(reservoir: MoneyReservoir): NewBalanceCheck =
      NewBalanceCheck(reservoir.code, HasReturnTo.getDomPathWithoutPrefix)
  }

  case class EditBalanceCheck private (balanceCheckId: Long, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditBalanceCheck {
    def apply(balanceCheck: BalanceCheck): EditBalanceCheck =
      EditBalanceCheck(balanceCheck.id, HasReturnTo.getDomPathWithoutPrefix)
  }
}
