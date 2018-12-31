package app.models.accounting.config

import hydro.models.access.DbQueryImplicits._

import app.common.Require.requireNonNull
import app.models.access.AppEntityAccess
import app.models.access.ModelFields
import hydro.models.access.ModelField
import app.models.accounting.{Transaction => AccountingTransaction}
import app.models.accounting.{TransactionGroup => AccountingTransactionGroup}
import app.models.user.User

import scala.collection.immutable.Seq
import scala.collection.immutable.Set

// Every field ending with "Tpl" may contain $-prefixed placeholders.
// Example: descriptionTpl = "Endowment for ${account.longName}"
case class Template(code: String,
                    name: String,
                    placement: Set[Template.Placement],
                    onlyShowForUserLoginNames: Option[Set[String]] = None,
                    zeroSum: Boolean,
                    iconClass: String,
                    transactions: Seq[Template.Transaction]) {
  requireNonNull(code, name, placement, onlyShowForUserLoginNames, zeroSum, iconClass, transactions)

  def showFor(location: Template.Placement, user: User)(implicit accountingConfig: Config,
                                                        entityAccess: AppEntityAccess): Boolean = {
    val showAtLocation = placement contains location
    val showToUser = onlyShowForUsers match {
      case Some(users) => users contains user
      case None        => true
    }
    showAtLocation && showToUser
  }

  def toPartial(account: Account)(implicit accountingConfig: Config): AccountingTransactionGroup.Partial = {
    AccountingTransactionGroup.Partial(
      transactions = transactions map (_.toPartial(account)),
      zeroSum = zeroSum)
  }

  private def onlyShowForUsers(implicit accountingConfig: Config,
                               entityAccess: AppEntityAccess): Option[Set[User]] = {
    onlyShowForUserLoginNames.map { loginNameOption =>
      loginNameOption.map { loginName =>
        val user = entityAccess.newQuerySyncForUser().findOne(ModelFields.User.loginName === loginName)
        require(user.isDefined, s"No user exists with loginName '$loginName'")
        require(
          accountingConfig.accountOf(user.get).isDefined,
          s"Only user names that have an associated account can be used in templates " +
            s"(user = '$loginName', template = '$name')"
        )
        user.get
      }
    }
  }
}

object Template {
  sealed abstract class Placement(code: String) {
    override def toString = code
  }
  object Placement {
    object EverythingView extends Placement("EVERYTHING_VIEW")
    object CashFlowView extends Placement("CASH_FLOW_VIEW")
    object LiquidationView extends Placement("LIQUIDATION_VIEW")
    object EndowmentsView extends Placement("ENDOWMENTS_VIEW")
    object SummaryView extends Placement("SUMMARY_VIEW")
    object TemplateList extends Placement("TEMPLATE_LIST")
    object SearchView extends Placement("SEARCH_VIEW")

    def fromString(string: String): Placement = string match {
      case "EVERYTHING_VIEW"  => EverythingView
      case "CASH_FLOW_VIEW"   => CashFlowView
      case "LIQUIDATION_VIEW" => LiquidationView
      case "ENDOWMENTS_VIEW"  => EndowmentsView
      case "SUMMARY_VIEW"     => SummaryView
      case "TEMPLATE_LIST"    => TemplateList
      case "SEARCH_VIEW"      => EverythingView
    }
  }

  case class Transaction(beneficiaryCodeTpl: Option[String] = None,
                         moneyReservoirCodeTpl: Option[String] = None,
                         categoryCodeTpl: Option[String] = None,
                         descriptionTpl: String = "",
                         flowInCents: Long = 0,
                         detailDescription: String = "",
                         tags: Seq[String] = Seq()) {
    requireNonNull(
      beneficiaryCodeTpl,
      moneyReservoirCodeTpl,
      categoryCodeTpl,
      descriptionTpl,
      flowInCents,
      tags)

    def toPartial(account: Account)(implicit accountingConfig: Config): AccountingTransaction.Partial = {
      def fillInPlaceholders(string: String): String = {
        val placeholderToReplacement = Map(
          "${account.code}" -> account.code,
          "${account.longName}" -> account.longName,
          "${account.defaultCashReservoir.code}" -> account.defaultCashReservoir.map(_.code).getOrElse(""),
          "${account.defaultElectronicReservoir.code}" -> account.defaultElectronicReservoir.code
        )
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
      AccountingTransaction.Partial(
        beneficiary = beneficiaryCodeTpl map fillInPlaceholders map accountingConfig.accounts,
        moneyReservoir = moneyReservoirCodeTpl map fillInPlaceholders map reservoirsIncludingNullMap,
        category = categoryCodeTpl map fillInPlaceholders map accountingConfig.categories,
        description = fillInPlaceholders(descriptionTpl),
        flowInCents = flowInCents,
        detailDescription = detailDescription,
        tags = tags
      )
    }
  }
}
