package flux.react.app.transactionviews

import japgolly.scalajs.react._
import common.Formatting._
import common.I18n
import common.time.Clock
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.Page
import flux.react.uielements
import flux.stores.entries.{EndowmentEntriesStoreFactory, GeneralEntry}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import models.{EntityAccess, User}
import models.accounting.config.{Account, Config}
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

final class Endowments(implicit entriesStoreFactory: EndowmentEntriesStoreFactory,
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
        uielements.Panel(i18n("facto.all-accounts")) {
          {
            for (account <- accountingConfig.personallySortedAccounts) yield {
              EntriesListTable[GeneralEntry, Account](
                tableTitle = i18n("facto.endowments-of", account.longName),
                tableClasses = Seq("table-endowments"),
                key = account.code,
                numEntriesStrategy = NumEntriesStrategy(start = 30, intermediateBeforeInf = Seq(100)),
                props = account,
                tableHeaders = Seq(
                  <.th(i18n("facto.payed")),
                  <.th(i18n("facto.consumed")),
                  <.th(i18n("facto.beneficiary")),
                  <.th(i18n("facto.payed-with-to")),
                  <.th(i18n("facto.category")),
                  <.th(i18n("facto.description")),
                  <.th(i18n("facto.flow")),
                  <.th("")
                ),
                calculateTableData = entry =>
                  Seq[VdomElement](
                    <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                    <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                    <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                    <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                    <.td(entry.categories.map(_.name).mkString(", ")),
                    <.td(uielements.DescriptionWithEntryCount(entry)),
                    <.td(uielements.MoneyWithCurrency(entry.flow)),
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
