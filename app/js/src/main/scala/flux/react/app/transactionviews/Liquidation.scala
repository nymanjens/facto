package flux.react.app.transactionviews

import common.Formatting._
import common.I18n
import common.time.Clock
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.Page
import flux.react.uielements
import flux.stores.entries.{AccountPair, GeneralEntry, LiquidationEntriesStoreFactory, LiquidationEntry}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.config.{Account, Config}
import models.accounting.money.ExchangeRateManager
import models.{EntityAccess, User}

import scala.collection.immutable.Seq

final class Liquidation(implicit entriesStoreFactory: LiquidationEntriesStoreFactory,
                        entityAccess: EntityAccess,
                        clock: Clock,
                        accountingConfig: Config,
                        user: User,
                        exchangeRateManager: ExchangeRateManager,
                        i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(
      (_, props) =>
        uielements.Panel(i18n("facto.all-combinations")) {
          {
            for {
              (account1, i1) <- accountingConfig.personallySortedAccounts.zipWithIndex
              (account2, i2) <- accountingConfig.personallySortedAccounts.zipWithIndex
              if i1 < i2
            } yield {
              EntriesListTable[LiquidationEntry, AccountPair](
                tableTitle = i18n("facto.debt-of", account1.longName, account2.longName),
                tableClasses = Seq("table-liquidation"),
                key = s"${account1.code}_${account2.code}",
                numEntriesStrategy = NumEntriesStrategy(start = 10, intermediateBeforeInf = Seq(30)),
                props = AccountPair(account1, account2),
                tableHeaders = Seq(
                  <.th(i18n("facto.payed")),
                  <.th(i18n("facto.beneficiary")),
                  <.th(i18n("facto.payed-with-to")),
                  <.th(i18n("facto.category")),
                  <.th(i18n("facto.description")),
                  <.th(i18n("facto.flow")),
                  <.th(s"${account1.veryShortName} -> ${account2.veryShortName}"),
                  <.th("")
                ),
                calculateTableData = entry =>
                  Seq[VdomElement](
                    <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                    <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                    <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                    <.td(entry.categories.map(_.name).mkString(", ")),
                    <.td(uielements.DescriptionWithEntryCount(entry)),
                    <.td(uielements.MoneyWithCurrency(entry.flow)),
                    <.td(uielements.MoneyWithCurrency(entry.debt)),
                    <.td(uielements.TransactionGroupEditButton(entry.groupId, props.router))
                )
              )
            }
          }.toVdomArray
      }
    )
    .build

  // **************** API ****************//
  def apply(router: RouterCtl[Page]): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterCtl[Page])
}
