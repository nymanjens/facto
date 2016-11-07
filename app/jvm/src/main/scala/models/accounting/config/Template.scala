package models.accounting.config

import scala.collection.immutable.Seq
import com.google.common.base.Preconditions.checkState
import common.Require.requireNonNullFields
import models.accounting.money.Money
import models.{User, Users}
import models.accounting.{TransactionGroupPartial, TransactionPartial}

// Every field ending with "Tpl" may contain $-prefixed placeholders.
// Example: descriptionTpl = "Endowment for ${account.longName}"
case class Template(code: String,
                    name: String,
                    private val placement: Set[Template.Placement],
                    private val onlyShowForUserLoginNames: Option[Set[String]],
                    private val zeroSum: Boolean,
                    val iconClass: String,
                    private val transactions: Seq[Template.Transaction]) {
  requireNonNullFields(this)

  def showFor(location: Template.Placement, user: User)(implicit accountingConfig: Config): Boolean = {
    val showAtLocation = placement contains location
    val showToUser = onlyShowForUsers match {
      case Some(users) => users contains user
      case None => true
    }
    showAtLocation && showToUser
  }

  def toPartial(account: Account)(implicit accountingConfig: Config): TransactionGroupPartial = {
    TransactionGroupPartial(
      transactions = transactions map (_.toPartial(account)),
      zeroSum = zeroSum)
  }

  private def onlyShowForUsers(implicit accountingConfig: Config): Option[Set[User]] = {
    onlyShowForUserLoginNames.map { loginNameOption =>
      loginNameOption.map { loginName =>
        val user = Users.findByLoginName(loginName)
        require(user.isDefined, s"No user exists with loginName '$loginName'")
        require(accountingConfig.accountOf(user.get).isDefined, s"Only user names that have an associated account can be used in templates " +
          s"(user = '$loginName', template = '$name')")
        user.get
      }
    }
  }
}

object Template {
  sealed trait Placement
  object Placement {
    object EverythingView extends Placement
    object CashFlowView extends Placement
    object LiquidationView extends Placement
    object EndowmentsView extends Placement
    object SummaryView extends Placement
    object TemplateList extends Placement
    object SearchView extends Placement

    def fromString(string: String): Placement = string match {
      case "EVERYTHING_VIEW" => EverythingView
      case "CASH_FLOW_VIEW" => CashFlowView
      case "LIQUIDATION_VIEW" => LiquidationView
      case "ENDOWMENTS_VIEW" => EndowmentsView
      case "SUMMARY_VIEW" => SummaryView
      case "TEMPLATE_LIST" => TemplateList
      case "SEARCH_VIEW" => EverythingView
    }
  }

  case class Transaction(beneficiaryCodeTpl: Option[String],
                         moneyReservoirCodeTpl: Option[String],
                         categoryCodeTpl: Option[String],
                         descriptionTpl: String,
                         flowInCents: Long,
                         tagsString: String) {
    requireNonNullFields(this)

    def toPartial(account: Account)(implicit accountingConfig: Config): TransactionPartial = {
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
        for (reservoir <- accountingConfig.visibleReservoirs(includeNullReservoir = true))
          yield reservoir.code -> reservoir
      }.toMap
      TransactionPartial(
        beneficiary = beneficiaryCodeTpl map fillInPlaceholders map accountingConfig.accounts,
        moneyReservoir = moneyReservoirCodeTpl map fillInPlaceholders map reservoirsIncludingNullMap,
        category = categoryCodeTpl map fillInPlaceholders map accountingConfig.categories,
        description = fillInPlaceholders(descriptionTpl),
        flowInCents = flowInCents,
        detailDescription = "",
        tagsString = tagsString)
    }
  }
}
