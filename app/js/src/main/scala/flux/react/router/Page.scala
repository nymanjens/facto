package flux.react.router

sealed trait Page
object Page {
  case object EverythingPage extends Page
  case object EndowmentsPage extends Page
  case object NewTransactionGroupPage extends Page
  case class EditTransactionGroupPage(transactionGroupId: Long) extends Page
  case object EverythingPage2 extends Page
}
