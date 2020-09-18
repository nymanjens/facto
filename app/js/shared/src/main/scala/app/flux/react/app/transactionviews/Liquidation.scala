package app.flux.react.app.transactionviews

import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.CollapseAllExpandAllButtons
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.router.AppPages
import app.flux.stores.entries.AccountPair
import app.flux.stores.entries.LiquidationEntry
import app.flux.stores.entries.factories.LiquidationEntriesStoreFactory
import app.flux.stores.CollapsedExpandedStateStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Account
import app.models.accounting.config.Config
import app.models.user.User
import hydro.common.Formatting._
import hydro.common.I18n
import hydro.common.time.Clock
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Liquidation(implicit
    entriesStoreFactory: LiquidationEntriesStoreFactory,
    collapsedExpandedStateStoreFactory: CollapsedExpandedStateStoreFactory,
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    accountingConfig: Config,
    user: User,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
) {

  private val entriesListTable: EntriesListTable[LiquidationEntry, AccountPair] = new EntriesListTable
  private val collapsedExpandedStateStoreHandle = collapsedExpandedStateStoreFactory
    .initializeView(getClass.getSimpleName, defaultExpanded = user.expandLiquidationTablesByDefault)

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState(State())
    .renderPS(($, props, state) => {
      implicit val router = props.router
      <.span(
        pageHeader.withExtension(router.currentPage)(
          CollapseAllExpandAllButtons(
            onExpandedUpdate = collapsedExpandedStateStoreHandle.setExpandedForAllTables
          ),
          " ",
          simplifyLiquidationButton(),
        ),
        Panel(i18n("app.all-combinations")) {
          {
            for {
              (account1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
              (account2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
              if i1 < i2
            } yield {
              val accountPair = AccountPair(account1, account2)
              val tableName = s"${account1.code}_${account2.code}"
              entriesListTable(
                tableTitle = i18n("app.debt-of", account1.longName, account2.longName),
                tableClasses = Seq("table-liquidation"),
                key = tableName,
                numEntriesStrategy = NumEntriesStrategy(
                  start = Liquidation.minNumEntriesPerPair,
                  intermediateBeforeInf = Seq(30),
                ),
                collapsedExpandedStateStore = Some(collapsedExpandedStateStoreHandle.getStore(tableName)),
                additionalInput = accountPair,
                latestEntryToTableTitleExtra = latestEntry => latestEntry.debt.toString,
                hideEmptyTable = true,
                tableHeaders = Seq(
                  <.th(i18n("app.payed")),
                  <.th(i18n("app.beneficiary")),
                  <.th(i18n("app.payed-with-to")),
                  <.th(i18n("app.category")),
                  <.th(i18n("app.description")),
                  <.th(i18n("app.flow")),
                  <.th(s"${account1.veryShortName} -> ${account2.veryShortName}"),
                  <.th(repayButton(account1 = account1, account2 = account2)),
                ),
                calculateTableData = entry =>
                  Seq[VdomElement](
                    <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                    <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                    <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                    <.td(entry.categories.map(_.name).mkString(", ")),
                    <.td(descriptionWithEntryCount(entry)),
                    <.td(uielements.MoneyWithCurrency(entry.flow)),
                    <.td(uielements.MoneyWithCurrency(entry.debt)),
                    <.td(uielements.TransactionGroupEditButton(entry.groupId)),
                  ),
              )
            }
          }.toVdomArray
        },
      )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private helper methods ****************//
  private def simplifyLiquidationButton()(implicit router: RouterContext): VdomElement = {
    val link = router.anchorWithHrefTo(AppPages.NewForLiquidationSimplification())
    Bootstrap.Button(tag = link)(
      Bootstrap.FontAwesomeIcon("scissors", fixedWidth = true),
      " ",
      i18n("app.simplify-liquidation"),
    )
  }

  private def repayButton(account1: Account, account2: Account)(implicit
      router: RouterContext
  ): VdomElement = {
    val link = router.anchorWithHrefTo(AppPages.NewForRepayment(account1 = account1, account2 = account2))
    Bootstrap.Button(Variant.info, Size.xs, tag = link)(
      Bootstrap.FontAwesomeIcon("check-square-o", fixedWidth = true),
      " ",
      i18n("app.repay"),
    )
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
  private case class State()
}

object Liquidation {
  val minNumEntriesPerPair: Int = 10
}
