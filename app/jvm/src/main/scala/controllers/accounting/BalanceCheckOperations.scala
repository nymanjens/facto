package controllers.accounting

import com.google.inject.Inject
import models.accounting.money.Money
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{AnyContent, Call, Controller, Request}
import play.twirl.api.Html
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

import common.{Clock, ReturnTo}
import models.User
import models.accounting.{BalanceCheck, BalanceChecks, Transactions, UpdateLogs}
import models.accounting.config.{MoneyReservoir, Config}
import controllers.helpers.AuthenticatedAction
import controllers.helpers.accounting.FormUtils.{validFlowAsFloat, flowAsFloatStringToCents}
import controllers.accounting.BalanceCheckOperations.{Forms, AddNewOperationMeta, EditOperationMeta, OperationMeta}

class BalanceCheckOperations @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  // ********** actions ********** //
  def addNewForm(moneyReservoirCode: String, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val moneyReservoir = Config.moneyReservoir(moneyReservoirCode)
      val initialData = Forms.BcData(
        issuerName = user.name,
        moneyReservoirName = moneyReservoir.name,
        balanceInCents = 0)
      Ok(formView(AddNewOperationMeta(moneyReservoirCode), initialData))
  }

  def editForm(bcId: Long, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val bc = BalanceChecks.findById(bcId)
      val formData = Forms.BcData.fromModel(bc)
      Ok(formView(EditOperationMeta(bcId), formData))
  }

  def addNew(moneyReservoirCode: String, returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addOrEdit(AddNewOperationMeta(moneyReservoirCode))
  }

  def edit(bcId: Long, returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addOrEdit(EditOperationMeta(bcId))
  }

  def delete(bcId: Long, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val bc = BalanceChecks.findById(bcId)
      UpdateLogs.addLog(user, UpdateLogs.Delete, bc)
      BalanceChecks.delete(bc)

      val moneyReservoirName = bc.moneyReservoir.name
      val message = Messages("facto.successfully-deleted-balance-check-for", moneyReservoirName)
      Redirect(returnTo).flashing("message" -> message)
  }

  // ********** package-private helper controllers ********** //
  private[accounting] def doAddConfirmation(moneyReservoirCode: String,
                                            balanceInCents: Long,
                                            mostRecentTransactionId: Long)
                                           (implicit user: User): Unit = {
    val moneyReservoir = Config.moneyReservoir(moneyReservoirCode)
    val mostRecentTransaction = Transactions.findById(mostRecentTransactionId)

    val balanceCheck = BalanceCheck(
      issuerId = user.id,
      moneyReservoirCode = moneyReservoir.code,
      balanceInCents = balanceInCents,
      checkDate = mostRecentTransaction.transactionDate)
    val persistedBc = BalanceChecks.add(balanceCheck)
    UpdateLogs.addLog(user, UpdateLogs.AddNew, persistedBc)
  }

  // ********** private helper controllers ********** //
  private def addOrEdit(operationMeta: OperationMeta)(implicit returnTo: ReturnTo) = AuthenticatedAction { implicit user =>
    implicit request =>
      // get sent data (copied from Form.bindFromRequest())
      val requestMap: Map[String, Seq[String]] = (request.body match {
        case body: AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
        case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
      }) ++ request.queryString

      Forms.balanceCheckForm.bindFromRequest(requestMap).fold(
        formWithErrors => {
          BadRequest(formView(operationMeta, formWithErrors))
        },
        bc => {
          persistBc(bc, operationMeta)

          val moneyReservoirName = operationMeta.moneyReservoir.name
          val message = operationMeta match {
            case _: AddNewOperationMeta => Messages("facto.successfully-created-a-balance-check-for", moneyReservoirName)
            case _: EditOperationMeta => Messages("facto.successfully-edited-a-balance-check-for", moneyReservoirName)
          }
          Redirect(returnTo.url).flashing("message" -> message)
        })
  }

  // ********** private helper methods ********** //
  private def persistBc(formData: Forms.BcData, operationMeta: OperationMeta)
                       (implicit user: User): Unit = {
    val balanceCheck = BalanceCheck(
      issuerId = user.id,
      moneyReservoirCode = operationMeta.moneyReservoir.code,
      balanceInCents = formData.balanceInCents,
      checkDate = formData.checkDate)
    val persistedBc = operationMeta match {
      case AddNewOperationMeta(_) =>
        BalanceChecks.add(balanceCheck)
      case EditOperationMeta(bcId) =>
        BalanceChecks.delete(BalanceChecks.findById(bcId))
        BalanceChecks.add(balanceCheck)
    }

    val operation = operationMeta match {
      case _: AddNewOperationMeta => UpdateLogs.AddNew
      case _: EditOperationMeta => UpdateLogs.Edit
    }
    UpdateLogs.addLog(user, operation, persistedBc)
  }

  private def formView(operationMeta: OperationMeta, formData: Forms.BcData)
                      (implicit user: User, request: Request[AnyContent], returnTo: ReturnTo): Html =
    formView(operationMeta, Forms.balanceCheckForm.fill(formData))


  private def formView(operationMeta: OperationMeta, form: Form[Forms.BcData])
                      (implicit user: User, request: Request[AnyContent], returnTo: ReturnTo): Html = {
    val title = operationMeta match {
      case _: AddNewOperationMeta => Messages("facto.new-balance-check")
      case _: EditOperationMeta => Messages("facto.edit-balance-check")
    }
    val formAction = operationMeta match {
      case AddNewOperationMeta(moneyReservoirCode) =>
        routes.BalanceCheckOperations.addNew(moneyReservoirCode) ++: returnTo
      case EditOperationMeta(bcId) => routes.BalanceCheckOperations.edit(bcId) ++: returnTo
    }
    val deleteAction = operationMeta.bcIdOption.map(bcId =>
      routes.BalanceCheckOperations.delete(bcId) ++: returnTo)
    views.html.accounting.balancecheckform(
      reservoir = operationMeta.moneyReservoir,
      title,
      bcForm = form,
      formAction,
      deleteAction)
  }
}

object BalanceCheckOperations {
  // ********** forms ********** //
  object Forms {

    // ********** form-data case classes ********** //
    case class BcData(issuerName: String,
                      moneyReservoirName: String,
                      checkDate: DateTime = Clock.now,
                      balanceInCents: Long = 0)

    object BcData {
      def fromModel(bc: BalanceCheck) = BcData(
        issuerName = bc.issuer.name,
        moneyReservoirName = bc.moneyReservoir.name,
        checkDate = bc.checkDate,
        balanceInCents = bc.balance.cents)
    }

    // ********** form classes ********** //
    val balanceCheckForm: Form[BcData] = Form(
      mapping(
        "issuerName" -> text,
        "moneyReservoirName" -> text,
        "checkDate" -> jodaDate("yyyy-MM-dd"),
        "balanceAsFloat" -> nonEmptyText.verifying(validFlowAsFloat).transform[Long](flowAsFloatStringToCents, Money.centsToFloatString)
      )(BcData.apply)(BcData.unapply)
    )
  }

  private[BalanceCheckOperations] sealed trait OperationMeta {
    def moneyReservoirCode: String

    val moneyReservoir = Config.moneyReservoir(moneyReservoirCode)

    def bcIdOption: Option[Long]
  }

  private[BalanceCheckOperations] case class AddNewOperationMeta(moneyReservoirCode: String) extends OperationMeta {
    override def bcIdOption = None
  }

  private[BalanceCheckOperations] case class EditOperationMeta(bcId: Long) extends OperationMeta {
    override def moneyReservoirCode = BalanceChecks.findById(bcId).moneyReservoir.code
    override def bcIdOption = Some(bcId)
  }
}
