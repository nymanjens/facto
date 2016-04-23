package controllers.accounting

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Controller, Call, Request, AnyContent}
import play.twirl.api.Html

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import common.Clock
import models.User
import models.accounting.{BalanceCheck, BalanceChecks, Transactions, Money, UpdateLogs}
import models.accounting.config.{MoneyReservoir, Config}
import controllers.Secured
import controllers.helpers.accounting.FormUtils.{validFlowAsFloat, flowAsFloatStringToMoney}

object BalanceCheckOperations extends Controller with Secured {

  // ********** actions ********** //
  def addNewForm(moneyReservoirCode: String, redirectTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      val moneyReservoir = Config.moneyReservoirs(moneyReservoirCode)
      val initialData = Forms.BcData(
        issuerName = user.name,
        moneyReservoirName = moneyReservoir.name,
        balance = Money(0))
      Ok(formView(AddNewOperationMeta(moneyReservoirCode), initialData, redirectTo))
  }

  def editForm(bcId: Long, redirectTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      val bc = BalanceChecks.all.findById(bcId)
      val formData = Forms.BcData.fromModel(bc)
      Ok(formView(EditOperationMeta(bcId), formData, redirectTo))
  }

  def addNew(moneyReservoirCode: String, redirectTo: String) =
    addOrEdit(AddNewOperationMeta(moneyReservoirCode), redirectTo)

  def addConfirmation(moneyReservoirCode: String, balanceInCents: Long, mostRecentTransactionId: Long, redirectTo: String) =
    ActionWithUser { implicit user =>
      implicit request =>
        val balance = Money(balanceInCents)
        val moneyReservoir = Config.moneyReservoirs(moneyReservoirCode)
        val mostRecentTransaction = Transactions.all.findById(mostRecentTransactionId)

        val balanceCheck = BalanceCheck(
          issuerId = user.id.get,
          moneyReservoirCode = moneyReservoir.code,
          balance = balance,
          checkDate = mostRecentTransaction.transactionDate)
        val persistedBc = BalanceChecks.all.save(balanceCheck)
        UpdateLogs.addLog(user, UpdateLogs.AddNew, persistedBc)

        val moneyReservoirName = moneyReservoir.name
        val message = s"Successfully added a balance check for $moneyReservoirName"
        Redirect(redirectTo).flashing("message" -> message)
    }

  def edit(bcId: Long, redirectTo: String) =
    addOrEdit(EditOperationMeta(bcId), redirectTo)

  def delete(bcId: Long, redirectTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      val bc = BalanceChecks.all.findById(bcId)
      UpdateLogs.addLog(user, UpdateLogs.Delete, bc)
      BalanceChecks.all.delete(bc)

      val moneyReservoirName = bc.moneyReservoir.name
      val message = s"Successfully deleted balance check for $moneyReservoirName"
      Redirect(redirectTo).flashing("message" -> message)
  }

  // ********** private helper controllers ********** //
  private def addOrEdit(operationMeta: OperationMeta, redirectTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      // get sent data (copied from Form.bindFromRequest())
      val requestMap: Map[String, Seq[String]] = (request.body match {
        case body: AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
        case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
      }) ++ request.queryString

      Forms.balanceCheckForm.bindFromRequest(requestMap).fold(
        formWithErrors => {
          BadRequest(formView(operationMeta, formWithErrors, redirectTo))
        },
        bc => {
          persistBc(bc, operationMeta)

          val moneyReservoirName = operationMeta.moneyReservoir.name
          val message = operationMeta match {
            case _: AddNewOperationMeta => s"Successfully created a balance check for $moneyReservoirName"
            case _: EditOperationMeta => s"Successfully edited a balance check for $moneyReservoirName"
          }
          Redirect(redirectTo).flashing("message" -> message)
        })
  }

  // ********** private helper methods ********** //
  private def persistBc(formData: Forms.BcData, operationMeta: OperationMeta)
                       (implicit user: User): Unit = {
    val balanceCheck = BalanceCheck(
      issuerId = user.id.get,
      moneyReservoirCode = operationMeta.moneyReservoir.code,
      balance = formData.balance,
      checkDate = formData.checkDate)
    val persistedBc = operationMeta match {
      case AddNewOperationMeta(_) =>
        BalanceChecks.all.save(balanceCheck)
      case EditOperationMeta(bcId) =>
        BalanceChecks.all.update(balanceCheck withId bcId)
    }

    val operation = operationMeta match {
      case _: AddNewOperationMeta => UpdateLogs.AddNew
      case _: EditOperationMeta => UpdateLogs.Edit
    }
    UpdateLogs.addLog(user, operation, persistedBc)
  }

  private def formView(operationMeta: OperationMeta, formData: Forms.BcData, redirectTo: String)
                      (implicit user: User, request: Request[AnyContent]): Html =
    formView(operationMeta, Forms.balanceCheckForm.fill(formData), redirectTo)


  private def formView(operationMeta: OperationMeta, form: Form[Forms.BcData], redirectTo: String)
                      (implicit user: User, request: Request[AnyContent]): Html = {
    val title = operationMeta match {
      case _: AddNewOperationMeta => "New Balance Check"
      case _: EditOperationMeta => "Edit Balance Check"
    }
    val formAction = operationMeta match {
      case AddNewOperationMeta(moneyReservoirCode) =>
        routes.BalanceCheckOperations.addNew(moneyReservoirCode, redirectTo)
      case EditOperationMeta(bcId) => routes.BalanceCheckOperations.edit(bcId, redirectTo)
    }
    val deleteAction = operationMeta.bcIdOption.map(bcId =>
      routes.BalanceCheckOperations.delete(bcId, redirectTo))
    views.html.accounting.balancecheckform(
      reservoir = operationMeta.moneyReservoir,
      title,
      bcForm = form,
      formAction,
      deleteAction)
  }

  // ********** forms ********** //
  object Forms {

    // ********** form-data case classes ********** //
    case class BcData(issuerName: String,
                      moneyReservoirName: String,
                      checkDate: DateTime = Clock.now,
                      balance: Money = Money(0))

    object BcData {
      def fromModel(bc: BalanceCheck) = BcData(
        issuerName = bc.issuer.name,
        moneyReservoirName = bc.moneyReservoir.name,
        checkDate = bc.checkDate,
        balance = bc.balance)
    }

    // ********** form classes ********** //
    val balanceCheckForm: Form[BcData] = Form(
      mapping(
        "issuerName" -> text,
        "moneyReservoirName" -> text,
        "checkDate" -> jodaDate("yyyy-MM-dd"),
        "balanceAsFloat" -> nonEmptyText.verifying(validFlowAsFloat).transform[Money](flowAsFloatStringToMoney, _.formatFloat)
      )(BcData.apply)(BcData.unapply)
    )
  }

  private sealed trait OperationMeta {
    def moneyReservoirCode: String

    val moneyReservoir = Config.moneyReservoirs(moneyReservoirCode)

    def bcIdOption: Option[Long]
  }

  private case class AddNewOperationMeta(moneyReservoirCode: String) extends OperationMeta {
    override def bcIdOption = None
  }

  private case class EditOperationMeta(bcId: Long) extends OperationMeta {
    override def moneyReservoirCode = BalanceChecks.all.findById(bcId).moneyReservoir.code
    override def bcIdOption = Some(bcId)
  }
}
