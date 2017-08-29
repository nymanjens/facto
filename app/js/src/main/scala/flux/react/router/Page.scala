package flux.react.router

import japgolly.scalajs.react.extra.router.Path
import models.accounting.{BalanceCheck, TransactionGroup}
import models.accounting.config.{Account, MoneyReservoir, Template}
import models.accounting.money.ReferenceMoney
import org.scalajs.dom

sealed trait Page
object Page {

  trait HasReturnTo {
    def returnToWithoutPrefix: String
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
  case class NewTransactionGroupPage(override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object NewTransactionGroupPage {
    def apply(): NewTransactionGroupPage = NewTransactionGroupPage(HasReturnTo.getDomPathWithoutPrefix)
  }
  case class EditTransactionGroupPage(transactionGroupId: Long, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object EditTransactionGroupPage {
    // Note: Getting ID here rather than TransactionGroup because we may not have fetched the TransactionGroup.
    def apply(transactionGroupId: Long): EditTransactionGroupPage =
      EditTransactionGroupPage(transactionGroupId, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewFromTemplatePage(templateCode: String, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object NewFromTemplatePage {
    def apply(template: Template): NewFromTemplatePage =
      NewFromTemplatePage(template.code, HasReturnTo.getDomPathWithoutPrefix)
  }
  case class NewForRepaymentPage(accountCode1: String,
                                 accountCode2: String,
                                 amountInCents: Long,
                                 override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object NewForRepaymentPage {
    def apply(account1: Account, account2: Account, amount: ReferenceMoney): NewForRepaymentPage =
      NewForRepaymentPage(account1.code, account2.code, amount.cents, HasReturnTo.getDomPathWithoutPrefix)
  }

  // Accounting forms - balance checks
  case class NewBalanceCheckPage private (reservoirCode: String, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object NewBalanceCheckPage {
    def apply(reservoir: MoneyReservoir): NewBalanceCheckPage =
      NewBalanceCheckPage(reservoir.code, HasReturnTo.getDomPathWithoutPrefix)
  }

  case class EditBalanceCheckPage private (balanceCheckId: Long, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object EditBalanceCheckPage {
    def apply(balanceCheck: BalanceCheck): EditBalanceCheckPage =
      EditBalanceCheckPage(balanceCheck.id, HasReturnTo.getDomPathWithoutPrefix)
  }
}
