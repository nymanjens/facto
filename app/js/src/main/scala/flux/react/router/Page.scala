package flux.react.router

sealed trait Page
object Page {

  case object RootPage extends Page

  // Accounting data views
  case object EverythingPage extends Page
  case object EndowmentsPage extends Page
  case object LiquidationPage extends Page

  // Accounting forms - transactions
  case object NewTransactionGroupPage extends Page
  case class EditTransactionGroupPage(transactionGroupId: Long) extends Page
  case class NewFromTemplatePage(templateCode: String) extends Page
  case class NewForRepaymentPage(accountCode1: String, accountCode2: String, amountInCents: Long)
      extends Page

  // Accounting forms - balance checks
  case object NewBalanceCheckPage extends Page
  case class EditBalanceCheckPage(balanceCheckId: Long) extends Page
}
