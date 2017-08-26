package flux.react.router

import japgolly.scalajs.react.extra.router.Path
import models.accounting.BalanceCheck
import models.accounting.config.MoneyReservoir
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
  case object NewTransactionGroupPage extends Page
  case class EditTransactionGroupPage(transactionGroupId: Long) extends Page
  case class NewFromTemplatePage(templateCode: String) extends Page
  case class NewForRepaymentPage(accountCode1: String, accountCode2: String, amountInCents: Long)
      extends Page

  // Accounting forms - balance checks
  case class NewBalanceCheckPage(reservoirCode: String, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object NewBalanceCheckPage {
    def apply(reservoir: MoneyReservoir): NewBalanceCheckPage =
      NewBalanceCheckPage(reservoir.code, HasReturnTo.getDomPathWithoutPrefix)
  }

  case class EditBalanceCheckPage(balanceCheckId: Long, override val returnToWithoutPrefix: String)
      extends Page
      with HasReturnTo
  object EditBalanceCheckPage {
    def apply(balanceCheck: BalanceCheck): EditBalanceCheckPage =
      EditBalanceCheckPage(balanceCheck.id, HasReturnTo.getDomPathWithoutPrefix)
  }
}
