package controllers.accounting

import common.ReturnTo
import models.accounting.money.{Money, DatedMoney, ReferenceMoney}

import scala.collection.{Seq => MutableSeq}
import scala.collection.immutable.Seq
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.mvc._
import play.twirl.api.Html
import play.api.i18n.Messages

// imports for 2.4 i18n (https://www.playframework.com/documentation/2.4.x/Migration24#I18n)
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._
import common.{Clock, ReturnTo}
import models.SlickUtils.dbApi._
import models.SlickUtils.{JodaToSqlDateMapper, dbRun}
import models.User
import models.accounting.{Transaction, Transactions, TransactionPartial, TransactionGroup, TransactionGroupPartial, TransactionGroups, UpdateLogs}
import models.accounting.config.{Config, Account, MoneyReservoir, Category, Template}
import controllers.helpers.AuthenticatedAction
import controllers.helpers.accounting.CashFlowEntry
import controllers.helpers.accounting.FormUtils.{validMoneyReservoirOrNullReservoir, validAccountCode, validCategoryCode,
validFlowAsFloat, flowAsFloatStringToCents, validTagsString, invalidWithMessageCode}

object TransactionGroupOperations extends Controller {

  // ********** actions ********** //
  def addNewForm(returnTo: String) = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    addNewFormFromPartial(TransactionPartial.from())
  }

  def editForm(transGroupId: Long, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val transGroup = TransactionGroups.findById(transGroupId)
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

  def delete(transGroupId: Long, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      val group = TransactionGroups.findById(transGroupId)
      val numTrans = group.transactions.size

      UpdateLogs.addLog(user, UpdateLogs.Delete, group)
      for (transaction <- group.transactions) {
        Transactions.delete(transaction)
      }
      TransactionGroups.delete(group)

      val message = if (numTrans == 1) {
        Messages("facto.successfully-deleted-1-transaction")
      } else {
        Messages("facto.successfully-deleted-transactions", numTrans)
      }
      Redirect(returnTo).flashing("message" -> message)
  }

  // ********** shortcuts ********** //
  def addNewFromTemplate(templateCode: String, returnTo: String) = AuthenticatedAction { implicit user =>
    implicit request =>
      implicit val returnToImplicit = ReturnTo(returnTo)

      val template = Config.templateWithCode(templateCode)
      // If this user is not associated with an account, it should not see any templates.
      val userAccount = Config.accountOf(user).get
      val partial = template.toPartial(userAccount)
      val initialData = Forms.TransGroupData.fromPartial(partial)
      Ok(formViewWithInitialData(AddNewOperationMeta(), initialData, templatesInNavbar = Seq(template)))
  }

  def addNewLiquidationRepayForm(accountCode1: String, accountCode2: String, amountInCents: Long, returnTo: String): AuthenticatedAction = {
    implicit val returnToImplicit = ReturnTo(returnTo)

    if (amountInCents < 0) {
      addNewLiquidationRepayForm(accountCode2, accountCode1, -amountInCents, returnTo)
    } else {
      val account1 = Config.accounts(accountCode1)
      val account2 = Config.accounts(accountCode2)
      def getAbsoluteFlowForAccountCurrency(account: Account): DatedMoney = {
        val amount = ReferenceMoney(amountInCents)
        val currency = account.defaultElectronicReservoir.currency
        // Using current date because the repayment takes place today.
        amount.withDate(Clock.now).exchangedForCurrency(currency)
      }

      addNewFormFromPartial(TransactionGroupPartial(Seq(
        TransactionPartial.from(
          beneficiary = account1,
          moneyReservoir = account1.defaultElectronicReservoir,
          category = Config.constants.accountingCategory,
          description = Config.constants.liquidationDescription,
          flowInCents = -getAbsoluteFlowForAccountCurrency(account1).cents),
        TransactionPartial.from(
          beneficiary = account1,
          moneyReservoir = account2.defaultElectronicReservoir,
          category = Config.constants.accountingCategory,
          description = Config.constants.liquidationDescription,
          flowInCents = getAbsoluteFlowForAccountCurrency(account2).cents)),
        zeroSum = true
      ))
    }
  }

  // ********** private helper controllers ********** //
  private def addNewFormFromPartial(partial: TransactionPartial)(implicit returnTo: ReturnTo): AuthenticatedAction =
    addNewFormFromPartial(TransactionGroupPartial(Seq(partial)))

  private def addNewFormFromPartial(partial: TransactionGroupPartial)
                                   (implicit returnTo: ReturnTo): AuthenticatedAction = AuthenticatedAction { implicit user =>
    implicit request =>
      val initialData = Forms.TransGroupData.fromPartial(partial)
      Ok(formViewWithInitialData(AddNewOperationMeta(), initialData))
  }

  private def addOrEdit(operationMeta: OperationMeta)(implicit returnTo: ReturnTo) =
    AuthenticatedAction { implicit user =>
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

        Forms.transactionGroupForm.forOperation(operationMeta).bindFromRequest(cleanedRequestMap).fold(
          formWithErrors => {
            BadRequest(formView(operationMeta, formWithErrors))
          },
          transGroup => {
            persistTransGroup(transGroup, operationMeta)

            val numTrans = transGroup.transactions.size
            val message = operationMeta match {
              case AddNewOperationMeta() => if (numTrans == 1) {
                Messages("facto.successfully-created-1-transaction")
              } else {
                Messages("facto.successfully-created-transactions", numTrans)
              }
              case EditOperationMeta(_) => if (numTrans == 1) {
                Messages("facto.successfully-edited-1-transaction")
              } else {
                Messages("facto.successfully-edited-transactions", numTrans)
              }
            }
            Redirect(returnTo.url).flashing("message" -> message)
          })
    }

  // ********** private helper methods ********** //
  private def persistTransGroup(transactionGroupData: Forms.TransGroupData, operationMeta: OperationMeta)
                               (implicit user: User): Unit = {
    val group = operationMeta match {
      case AddNewOperationMeta() => TransactionGroups.add(TransactionGroup())
      case EditOperationMeta(transGroupId) => TransactionGroups.findById(transGroupId)
    }

    // reomve existing transactions in this group
    for (transaction <- group.transactions) {
      Transactions.delete(transaction)
    }

    for (trans <- transactionGroupData.transactions) {
      Transactions.add(Transaction(
        transactionGroupId = group.id,
        issuerId = user.id,
        beneficiaryAccountCode = trans.beneficiaryAccountCode,
        moneyReservoirCode = trans.moneyReservoirCode,
        categoryCode = trans.categoryCode,
        description = trans.description,
        flowInCents = trans.flowInCents,
        detailDescription = trans.detailDescription,
        tagsString = trans.tagsString,
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
    formView(operationMeta, Forms.transactionGroupForm.forOperation(operationMeta).fill(formData), templatesInNavbar)


  private def formView(operationMeta: OperationMeta,
                       form: Form[Forms.TransGroupData],
                       templatesInNavbar: Seq[Template] = Seq())
                      (implicit user: User, request: Request[AnyContent], returnTo: ReturnTo): Html = {
    val title = operationMeta match {
      case AddNewOperationMeta() => Messages("facto.new-transaction")
      case EditOperationMeta(_) => Messages("facto.edit-transaction")
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
                               flowInCents: Long = 0,
                               detailDescription: String = "",
                               tagsString: String = "",
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
          flowInCents = trans.flowInCents,
          detailDescription = trans.detailDescription,
          tagsString = trans.tagsString)
      }

      def fromModel(trans: Transaction) = TransactionData(
        issuerName = trans.issuer.name,
        beneficiaryAccountCode = trans.beneficiaryAccountCode,
        moneyReservoirCode = trans.moneyReservoirCode,
        categoryCode = trans.categoryCode,
        description = trans.description,
        flowInCents = trans.flow.cents,
        detailDescription = trans.detailDescription,
        tagsString = trans.tagsString,
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
    val transactionGroupForm = new Object {
      def forOperation(operationMeta: OperationMeta): Form[TransGroupData] = operationMeta match {
        case AddNewOperationMeta() =>
          Form(formMapping verifying uniqueTransaction)
        case EditOperationMeta(_) =>
          Form(formMapping)
      }

      private val formMapping: Mapping[TransGroupData] = mapping(
        "transactions" -> seq(
          mapping(
            "issuerName" -> text,
            "beneficiaryAccountCode" -> nonEmptyText.verifying(validAccountCode),
            "moneyReservoirCode" -> text.verifying(validMoneyReservoirOrNullReservoir),
            "categoryCode" -> nonEmptyText.verifying(validCategoryCode),
            "description" -> nonEmptyText,
            "flowAsFloat" -> nonEmptyText.verifying(validFlowAsFloat).transform[Long](flowAsFloatStringToCents, Money.centsToFloatString),
            "detailDescription" -> text,
            "tags" -> text.verifying(validTagsString),
            "transactionDate" -> jodaDate("yyyy-MM-dd"),
            "consumedDate" -> jodaDate("yyyy-MM-dd")
          )(TransactionData.apply)(TransactionData.unapply)
        ),
        "zeroSum" -> boolean
      )(TransGroupData.apply)(TransGroupData.unapply) verifying validMoneyReservoirs verifying noFutureForeignTransactions

      private def validMoneyReservoirs = Constraint[TransGroupData]((groupData: TransGroupData) => {
        val containsEmptyReservoirCodes = groupData.transactions.exists(_.moneyReservoirCode == "")
        val allReservoirCodesAreEmpty = !groupData.transactions.exists(_.moneyReservoirCode != "")
        val totalFlowInCents = groupData.transactions.map(_.flowInCents).sum

        groupData.transactions.size match {
          case 0 => throw new AssertionError("Should not be possible")
          case 1 if containsEmptyReservoirCodes => invalidWithMessageCode("facto.error.noReservoir.atLeast2")
          case 1 => Valid
          case _ if allReservoirCodesAreEmpty =>
            if (totalFlowInCents == 0) {
              Valid
            } else {
              invalidWithMessageCode("facto.error.noReservoir.zeroSum")
            }
          case _ if containsEmptyReservoirCodes => invalidWithMessageCode("facto.error.noReservoir.notAllTheSame")
          case _ => Valid
        }
      })

      // Don't allow future transactions in a foreign currency because we don't know what the exchange rate
      // to the default currency will be. Future fluctuations might break the immutability of the conversion.
      private def noFutureForeignTransactions = Constraint[TransGroupData]((groupData: TransGroupData) => {
        val futureForeignTransactions = groupData.transactions.filter { transactionData =>
          val foreignCurrency = Config.moneyReservoir(transactionData.moneyReservoirCode).currency.isForeign
          val dateInFuture = transactionData.transactionDate > Clock.now
          foreignCurrency && dateInFuture
        }
        if (futureForeignTransactions.isEmpty) {
          Valid
        } else {
          invalidWithMessageCode("facto.error.foreignReservoirInFuture")
        }
      })

      // Don't allow creation of duplicate transactions because they are probably unintended (e.g. pressing enter twice).
      private def uniqueTransaction = Constraint[TransGroupData]((groupData: TransGroupData) => {
        def fetchMatchingTransaction(transactionData: TransactionData): Option[Transaction] = {
          val possibleMatches = dbRun(
            Transactions.newQuery
              .filter(_.transactionDate === transactionData.transactionDate)
              .filter(_.categoryCode === transactionData.categoryCode))

          possibleMatches.view
            .filter(trans => TransactionData.fromModel(trans) == transactionData).headOption
        }

        val matches = groupData.transactions.toStream.map(fetchMatchingTransaction)
        if (matches.filter(_ == None).isEmpty) {
          // Matching transaction present for all transactionDatas
          if (matches.map(_.get.transactionGroupId).toSet.size == 1) {
            // All same group
            invalidWithMessageCode("facto.error.duplicateTransactionGroup")
          } else {
            Valid
          }
        } else {
          Valid
        }
      })
    }
  }

  private[TransactionGroupOperations] sealed trait OperationMeta
  private[TransactionGroupOperations] case class AddNewOperationMeta() extends OperationMeta
  private[TransactionGroupOperations] case class EditOperationMeta(transGroupId: Long) extends OperationMeta
}
