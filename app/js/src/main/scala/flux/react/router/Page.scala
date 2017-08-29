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

  case object RootPage extends Page

  // Accounting data views
  case object EverythingPage extends Page
  case object CashFlowPage extends Page
  case object LiquidationPage extends Page
  case object EndowmentsPage extends Page

  // Accounting forms - transactions
  case class NewTransactionGroupPage private (returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewTransactionGroupPage {
    def apply(): NewTransactionGroupPage = NewTransactionGroupPage(HasReturnTo.getDomPathWithoutPrefix)
  }
  case class EditTransactionGroupPage private (transactionGroupId: Long, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditTransactionGroupPage {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long): EditTransactionGroupPage =
      EditTransactionGroupPage(transactionGroupId, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewFromTemplatePage private (templateCode: String, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewFromTemplatePage {
    def apply(template: Template): NewFromTemplatePage =
      NewFromTemplatePage(template.code, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewForRepaymentPage private (accountCode1: String,
                                          accountCode2: String,
                                          amountInCents: Long,
                                          returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewForRepaymentPage {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney): NewForRepaymentPage =
      NewForRepaymentPage(account1.code, account2.code, amount.cents, HasReturnTo.getDomPathWithoutPrefix)
  }

  // Accounting forms - balance checks
  case class NewBalanceCheckPage private (reservoirCode: String, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object NewBalanceCheckPage {
    def apply(reservoir: MoneyReservoir): NewBalanceCheckPage =
      NewBalanceCheckPage(reservoir.code, HasReturnTo.getDomPathWithoutPrefix)
  }

  case class EditBalanceCheckPage private (balanceCheckId: Long, returnToWithoutPrefix: String)
      extends HasReturnTo(returnToWithoutPrefix)
      with Page
  object EditBalanceCheckPage {
    def apply(balanceCheck: BalanceCheck): EditBalanceCheckPage =
      EditBalanceCheckPage(balanceCheck.id, HasReturnTo.getDomPathWithoutPrefix)
  }
}
