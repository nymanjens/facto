package controllers.accounting

import common.ReturnTo

import scala.collection.{Seq => MutableSeq}
import scala.collection.immutable.Seq
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.mvc._
import play.twirl.api.Html

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import org.joda.time.DateTime

import common.{Clock, ReturnTo}
import models.User
import models.accounting.{Transaction, Transactions, TransactionPartial, TransactionGroup, TransactionGroupPartial, TransactionGroups, Money, UpdateLogs}
import models.accounting.config.{Config, Account, MoneyReservoir, Category, Template}
import controllers.Secured
import controllers.helpers.accounting.CashFlowEntry
import controllers.helpers.accounting.FormUtils.{validMoneyReservoirOrNullReservoir, validAccountCode, validCategoryCode, validFlowAsFloat, flowAsFloatStringToMoney, invalidWithMessageCode}

object TransactionGroupOperations extends Controller with Secured {

  // ********** actions ********** //
  def addNewForm(returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addNewFormFromPartial(TransactionPartial.from())
  }

  def editForm(transGroupId: Long, returnTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val transGroup = TransactionGroups.all.findById(transGroupId)
      val formData = Forms.TransGroupData.fromModel(transGroup)
      Ok(formViewWithInitialData(EditOperationMeta(transGroupId), formData))
  }

  def addNew(returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addOrEdit(AddNewOperationMeta())
  }

  def edit(transGroupId: Long, returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addOrEdit(EditOperationMeta(transGroupId))
  }

  def delete(transGroupId: Long, returnTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      val group = TransactionGroups.all.findById(transGroupId)
      val numTrans = group.transactions.size

      UpdateLogs.addLog(user, UpdateLogs.Delete, group)
      for (transaction <- group.transactions) {
        Transactions.all.delete(transaction)
      }
      TransactionGroups.all.delete(group)

      val message = s"""Successfully deleted ${numTrans} transaction${if (numTrans == 1) "" else "s"}"""
      Redirect(returnTo).flashing("message" -> message)
  }

  // ********** shortcuts ********** //
  def addNewFromTemplate(templateId: Long, returnTo: String) = ActionWithUser { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val template = Config.templateWithId(templateId)
      // If this user is not associated with an account, it should not see any templates.
      val userAccount = Config.accountOf(user).get
      val partial = template.toPartial(userAccount)
      val initialData = Forms.TransGroupData.fromPartial(partial)
      Ok(formViewWithInitialData(AddNewOperationMeta(), initialData, templatesInNavbar = Seq(template)))
  }

  def addNewLiquidationRepayForm(accountCode1: String, accountCode2: String, amountInCents: Long, returnTo: String): EssentialAction = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    val amount = Money(amountInCents)
    if (amount < Money(0)) {
      addNewLiquidationRepayForm(accountCode2, accountCode1, -amount.cents, returnTo)
    } else {
      val account1 = Config.accounts(accountCode1)
      val account2 = Config.accounts(accountCode2)
      addNewFormFromPartial(TransactionGroupPartial(Seq(
        TransactionPartial.from(
          beneficiary = account1,
          moneyReservoir = account1.defaultElectronicReservoir,
          category = Config.constants.accountingCategory,
          description = Config.constants.liquidationDescription,
          flow = amount.negated),
        TransactionPartial.from(
          beneficiary = account1,
          moneyReservoir = account2.defaultElectronicReservoir,
          category = Config.constants.accountingCategory,
          description = Config.constants.liquidationDescription,
          flow = amount)),
        zeroSum = true
      ))
    }
  }

  // ********** private helper controllers ********** //
  private def addNewFormFromPartial(partial: TransactionPartial)(implicit returnTo: ReturnTo): EssentialAction =
    addNewFormFromPartial(TransactionGroupPartial(Seq(partial)))

  private def addNewFormFromPartial(partial: TransactionGroupPartial)
                                   (implicit returnTo: ReturnTo): EssentialAction = ActionWithUser { implicit user =>
    implicit request =>
      val initialData = Forms.TransGroupData.fromPartial(partial)
      Ok(formViewWithInitialData(AddNewOperationMeta(), initialData))
  }

  private def addOrEdit(operationMeta: OperationMeta)(implicit returnTo: ReturnTo) =
    ActionWithUser { implicit user =>
      implicit request =>
        val cleanedRequestMap: Map[String, MutableSeq[String]] = {
          // get sent data (copied from Form.bindFromRequest())
          val requestMap: Map[String, MutableSeq[String]] = (request.body match {
            case body: AnyContent if body.asFormUrlEncoded.isDefined => body.asFormUrlEncoded.get
            case body: Map[_, _] => body.asInstanceOf[Map[String, Seq[String]]]
          }) ++ request.queryString

          /** fix for non-consecutive transaction numbers **/
          val otherExpectedFields = Set("zeroSum", "returnTo")
          // find transactionNumMapping
          def extractTransNum(key: String): Int = {
            val transNumRegex = """\w+\[(\d+)\].*""".r
            key match {
              case transNumRegex(num) => num.toInt
            }
          }
          val transactionNumMapping = requestMap.keys
            .filter(!otherExpectedFields.contains(_))
            .map(extractTransNum(_))
            .toList
            .sorted
            .zipWithIndex
            .toMap

          // clean requestMap
          def applyTransactionNumMappingToKey(k: String) = {
            if (otherExpectedFields.contains(k)) {
              k
            } else {
              val transNum = extractTransNum(k)
              val mappedTransNum = transactionNumMapping(transNum)
              k.replace(s"[$transNum]", s"[$mappedTransNum]")
            }
          }
          requestMap.map {
            case (k, v) => applyTransactionNumMappingToKey(k) -> v
          }
        }

        Forms.transactionGroupForm.bindFromRequest(cleanedRequestMap).fold(
          formWithErrors => {
            BadRequest(formView(operationMeta, formWithErrors))
          },
          transGroup => {
            persistTransGroup(transGroup, operationMeta)

            val numTrans = transGroup.transactions.size
            val message = operationMeta match {
              case AddNewOperationMeta() => s"""Successfully created ${numTrans} transaction${if (numTrans == 1) "" else "s"}"""
              case EditOperationMeta(_) => s"""Successfully edited ${numTrans} transaction${if (numTrans == 1) "" else "s"}"""
            }
            Redirect(returnTo.url).flashing("message" -> message)
          })
    }

  // ********** private helper methods ********** //
  private def persistTransGroup(transactionGroupData: Forms.TransGroupData, operationMeta: OperationMeta)
                               (implicit user: User): Unit = {
    val group = operationMeta match {
      case AddNewOperationMeta() => TransactionGroups.all.save(TransactionGroup())
      case EditOperationMeta(transGroupId) => TransactionGroups.all.findById(transGroupId)
    }

    // reomve existing transactions in this group
    for (transaction <- group.transactions) {
      Transactions.all.delete(transaction)
    }

    for (trans <- transactionGroupData.transactions) {
      Transactions.all.save(Transaction(
        transactionGroupId = group.id.get,
        issuerId = user.id.get,
        beneficiaryAccountCode = trans.beneficiaryAccountCode,
        moneyReservoirCode = trans.moneyReservoirCode,
        categoryCode = trans.categoryCode,
        description = trans.description,
        flow = trans.flow,
        detailDescription = trans.detailDescription,
        createdDate = group.createdDate,
        transactionDate = trans.transactionDate,
        consumedDate = trans.consumedDate))
    }

    val operation = operationMeta match {
      case _: AddNewOperationMeta => UpdateLogs.AddNew
      case _: EditOperationMeta => UpdateLogs.Edit
    }
    UpdateLogs.addLog(user, operation, group)
  }

  private def formViewWithInitialData(operationMeta: OperationMeta,
                                      formData: Forms.TransGroupData,
                                      templatesInNavbar: Seq[Template] = Seq())
                                     (implicit user: User, request: Request[AnyContent], returnTo: ReturnTo): Html =
    formView(operationMeta, Forms.transactionGroupForm.fill(formData), templatesInNavbar)


  private def formView(operationMeta: OperationMeta,
                       form: Form[Forms.TransGroupData],
                       templatesInNavbar: Seq[Template] = Seq())
                      (implicit user: User, request: Request[AnyContent], returnTo: ReturnTo): Html = {
    val title = operationMeta match {
      case AddNewOperationMeta() => "New Transaction"
      case EditOperationMeta(_) => "Edit Transaction"
    }
    val formAction: Call = operationMeta match {
      case AddNewOperationMeta() => routes.TransactionGroupOperations.addNew() ++: returnTo
      case EditOperationMeta(transGroupId) => routes.TransactionGroupOperations.edit(transGroupId) ++: returnTo
    }
    val deleteAction = operationMeta match {
      case AddNewOperationMeta() => None
      case EditOperationMeta(transGroupId) => Some(routes.TransactionGroupOperations.delete(transGroupId) ++: returnTo)
    }
    views.html.accounting.transactiongroupform(
      title,
      transGroupForm = form,
      formAction = formAction,
      deleteAction = deleteAction,
      templatesInNavbar = templatesInNavbar)
  }

  // ********** forms ********** //
  object Forms {

    // ********** form-data case classes ********** //
    case class TransactionData(issuerName: String,
                               beneficiaryAccountCode: String = "",
                               moneyReservoirCode: String = "",
                               categoryCode: String = "",
                               description: String = "",
                               flow: Money = Money(0),
                               detailDescription: String = "",
                               transactionDate: DateTime = Clock.now,
                               consumedDate: DateTime = Clock.now)

    object TransactionData {

      def fromPartial(trans: TransactionPartial)(implicit user: User) = {
        val beneficiary = trans.beneficiary.getOrElse(Config.accounts.values.head)
        val moneyReservoir = trans.moneyReservoir.getOrElse(Config.visibleReservoirs.filter(_.owner == beneficiary).head)
        TransactionData(
          issuerName = user.name,
          beneficiaryAccountCode = beneficiary.code,
          moneyReservoirCode = moneyReservoir.code,
          categoryCode = trans.category.map(_.code).getOrElse(""),
          description = trans.description,
          flow = trans.flow,
          detailDescription = trans.detailDescription)
      }

      def fromModel(trans: Transaction) = TransactionData(
        issuerName = trans.issuer.name,
        beneficiaryAccountCode = trans.beneficiaryAccountCode,
        moneyReservoirCode = trans.moneyReservoirCode,
        categoryCode = trans.categoryCode,
        description = trans.description,
        flow = trans.flow,
        detailDescription = trans.detailDescription,
        transactionDate = trans.transactionDate,
        consumedDate = trans.consumedDate)
    }

    case class TransGroupData(transactions: MutableSeq[TransactionData], zeroSum: Boolean = false)

    object TransGroupData {
      def fromPartial(transGroup: TransactionGroupPartial)(implicit user: User) =
        TransGroupData(transGroup.transactions.map(TransactionData.fromPartial(_)), transGroup.zeroSum)

      def fromModel(transGroup: TransactionGroup) =
        TransGroupData(
          transGroup.transactions.map(TransactionData.fromModel(_)),
          zeroSum = transGroup.isZeroSum)
    }

    // ********** form classes ********** //
    val transactionGroupForm: Form[TransGroupData] = Form(
      mapping(
        "transactions" -> seq(
          mapping(
            "issuerName" -> text,
            "beneficiaryAccountCode" -> nonEmptyText.verifying(validAccountCode),
            "moneyReservoirCode" -> text.verifying(validMoneyReservoirOrNullReservoir),
            "categoryCode" -> nonEmptyText.verifying(validCategoryCode),
            "description" -> nonEmptyText,
            "flowAsFloat" -> nonEmptyText.verifying(validFlowAsFloat).transform[Money](flowAsFloatStringToMoney, _.formatFloat),
            "detailDescription" -> text,
            "transactionDate" -> jodaDate("yyyy-MM-dd"),
            "consumedDate" -> jodaDate("yyyy-MM-dd")
          )(TransactionData.apply)(TransactionData.unapply)
        ),
        "zeroSum" -> boolean
      )(TransGroupData.apply)(TransGroupData.unapply) verifying Constraint[TransGroupData]("error.invalid")(groupData => {
        val containsEmptyReservoirCodes = groupData.transactions.exists(_.moneyReservoirCode == "")
        val allReservoirCodesAreEmpty = !groupData.transactions.exists(_.moneyReservoirCode != "")
        val totalFlow = groupData.transactions.map(_.flow).sum

        groupData.transactions.size match {
          case 0 => throw new AssertionError("Should not be possible")
          case 1 if containsEmptyReservoirCodes => invalidWithMessageCode("facto.error.noReservoir.atLeast2")
          case 1 => Valid
          case _ if allReservoirCodesAreEmpty =>
            if (totalFlow == Money(0)) {
              Valid
            } else {
              invalidWithMessageCode("facto.error.noReservoir.zeroSum")
            }
          case _ if containsEmptyReservoirCodes => invalidWithMessageCode("facto.error.noReservoir.notAllTheSame")
          case _ => Valid
        }
      })
    )
  }

  private sealed trait OperationMeta

  private case class AddNewOperationMeta() extends OperationMeta

  private case class EditOperationMeta(transGroupId: Long) extends OperationMeta

}
