package models.accounting.config

import scala.collection.immutable.Seq

import com.google.common.base.Preconditions.checkState

import common.Require.requireNonNullFields
import models.{Users, User}
import models.accounting.{TransactionGroupPartial, TransactionPartial, Money}

// Every field ending with "Tpl" may contain $-prefixed placeholders.
// Example: descriptionTpl = "Endowment for ${account.longName}"
case class Template(id: Long,
                    name: String,
                    private val placement: Set[Template.Placement],
                    private val onlyShowForUserLoginNames: Option[Set[String]],
                    private val zeroSum: Boolean,
                    private val transactions: Seq[Template.Transaction]) {
  requireNonNullFields(this)
  validateLoginNames()

  def showFor(location: Template.Placement, user: User): Boolean = {
    val showAtLocation = placement contains location
    val showToUser = onlyShowForUsers match {
      case Some(users) => users contains user
      case None => true
    }
    showAtLocation && showToUser
  }

  def toPartial(account: Account): TransactionGroupPartial = {
    TransactionGroupPartial(
      transactions = transactions map (_.toPartial(account)),
      zeroSum = zeroSum)
  }

  private def onlyShowForUsers: Option[Set[User]] = {
    onlyShowForUserLoginNames.map { loginNameOption =>
      loginNameOption.map { loginName =>
        Users.findByLoginName(loginName).get // This will exist because earlier check in validateLoginNames() succeeded.
      }
    }
  }

  private def validateLoginNames(): Unit = {
    onlyShowForUserLoginNames.map { loginNameOption =>
      loginNameOption.map { loginName =>
        val user = Users.findByLoginName(loginName)
        require(user.isDefined, s"No user exists with loginName '$loginName'")
        require(Config.accountOf(user.get).isDefined, s"Only user names that have an associated account can be used in templates " +
          s"(user = '$loginName', template = '$name')")
      }
    }
  }
}

object Template {
  sealed trait Placement
  object Placement {
    object GeneralView extends Placement
    object CashFlowView extends Placement
    object LiquidationView extends Placement
    object EndowmentsView extends Placement
    object SummaryView extends Placement
    object TemplateList extends Placement

    def fromString(string: String): Placement = string match {
      case "GENERAL_VIEW" => GeneralView
      case "CASH_FLOW_VIEW" => CashFlowView
      case "LIQUIDATION_VIEW" => LiquidationView
      case "ENDOWMENTS_VIEW" => EndowmentsView
      case "SUMMARY_VIEW" => SummaryView
      case "TEMPLATE_LIST" => TemplateList
    }
  }

  case class Transaction(beneficiaryCodeTpl: Option[String],
                         moneyReservoirCodeTpl: Option[String],
                         categoryCodeTpl: Option[String],
                         descriptionTpl: String,
                         flowInCents: Long) {
    requireNonNullFields(this)

    def toPartial(account: Account): TransactionPartial = {
      def fillInPlaceholders(string: String): String = {
        val placeholderToReplacement = Map(
          "${account.code}" -> account.code,
          "${account.longName}" -> account.longName,
          "${account.defaultCashReservoir.code}" -> account.defaultCashReservoir.map(_.code).getOrElse(""),
          "${account.defaultElectronicReservoir.code}" -> account.defaultElectronicReservoir.code)
        var result = string
        for ((placeholder, replacement) <- placeholderToReplacement) {
          result = result.replace(placeholder, replacement)
        }
        result
      }
      val reservoirsIncludingNullMap = {
        for (reservoir <- Config.visibleReservoirs(includeNullReservoir = true))
          yield reservoir.code -> reservoir
      }.toMap
      TransactionPartial(
        beneficiary = beneficiaryCodeTpl map fillInPlaceholders map Config.accounts,
        moneyReservoir = moneyReservoirCodeTpl map fillInPlaceholders map reservoirsIncludingNullMap,
        category = categoryCodeTpl map fillInPlaceholders map Config.categories,
        description = fillInPlaceholders(descriptionTpl),
        flow = Money(flowInCents),
        detailDescription = "")
    }
  }
}
