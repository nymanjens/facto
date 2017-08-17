package flux.react.app.transactionviews
import scala.scalajs.js
import flux.react.ReactVdomUtils.<<
import common.Formatting._
import common.I18n
import common.LoggingUtils.LogExceptionsCallback
import common.time.Clock
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.router.Page
import flux.react.uielements
import flux.stores.entries.{AccountPair, CashFlowEntriesStoreFactory, CashFlowEntry}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import models.accounting.BalanceCheck
import models.accounting.config.{Account, Config, MoneyReservoir}
import models.accounting.money.{ExchangeRateManager, ReferenceMoney}
import models.{EntityAccess, User}

import scala.collection.immutable.Seq

final class CashFlow(implicit entriesStoreFactory: CashFlowEntriesStoreFactory,
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
        <.span({
          for (account <- accountingConfig.personallySortedAccounts) yield {
            uielements.Panel(i18n("facto.account-of", account.longName), key = account.code) {
              {
                for {
                  reservoir <- accountingConfig
                    .moneyReservoirs(
                      includeNullReservoir = false,
                      includeHidden = props.includeHiddenReservoirs)
                  if reservoir.owner == account
                } yield {
                  EntriesListTable.withRowNumber[CashFlowEntry, MoneyReservoir](
                    tableTitle = reservoir.name,
                    tableClasses = Seq("table-cashflow"),
                    key = reservoir.code,
                    numEntriesStrategy = NumEntriesStrategy(start = 10, intermediateBeforeInf = Seq(30)),
                    props = reservoir,
                    tableHeaders = Seq(
                      <.th(i18n("facto.payed")),
                      <.th(i18n("facto.consumed")),
                      <.th(i18n("facto.beneficiary")),
                      <.th(i18n("facto.category")),
                      <.th(i18n("facto.description")),
                      <.th(i18n("facto.flow")),
                      <.th(i18n("facto.balance")),
                      <.th(balanceCheckAddNewButton(reservoir, router = props.router))
                    ),
                    calculateTableDataFromEntryAndRowNum = (cashFlowEntry, rowNumber) =>
                      cashFlowEntry match {
                        case entry: CashFlowEntry.RegularEntry =>
                          Seq[VdomElement](
                            <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                            <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                            <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                            <.td(entry.categories.map(_.name).mkString(", ")),
                            <.td(uielements.DescriptionWithEntryCount(entry)),
                            <.td(uielements.MoneyWithCurrency(entry.flow)),
                            <.td(
                              uielements.MoneyWithCurrency(entry.balance),
                              <<.ifThen(entry.balanceVerified)(<.i(^.className := "fa fa-check fa-fw")),
                              <<.ifThen(!entry.balanceVerified && rowNumber == 0)(
                                balanceCheckConfirmButton(reservoir, entry, props.router))
                            ),
                            <.td(uielements.TransactionGroupEditButton(entry.groupId, props.router))
                          )
                        case CashFlowEntry.BalanceCorrection(balanceCorrection) =>
                          Seq[VdomElement](
                            <.td(formatDate(balanceCorrection.checkDate)),
                            <.td(
                              ^.colSpan := 5,
                              ^.style := js.Dictionary("fontWeight" -> "bold"),
                              i18n("facto.balance-correction") + ":"),
                            <.td(uielements.MoneyWithCurrency(balanceCorrection.balance)),
                            <.td(balanceCheckEditButton(balanceCorrection, props.router))
                          )
                    }
                  )
                }
              }.toVdomArray
            }
          }
        }.toVdomArray)
    )
    .build

  // **************** API ****************//
  def apply(router: RouterCtl[Page], includeHiddenReservoirs: Boolean): VdomElement = {
    component(Props(includeHiddenReservoirs, router))
  }

  // **************** Private helper methods ****************//
  private def balanceCheckAddNewButton(reservoir: MoneyReservoir, router: RouterCtl[Page]): VdomElement = {
    <.a(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw"),
      i18n("facto.set"))
  }

  private def balanceCheckConfirmButton(reservoir: MoneyReservoir,
                                        entry: CashFlowEntry.RegularEntry,
                                        router: RouterCtl[Page]): VdomElement = {
    <.button(
      ^.className := "btn btn-info btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-check-square-o fa-fw"))
  }

  def balanceCheckEditButton(balanceCorrection: BalanceCheck, router: RouterCtl[Page]): VdomElement = {
    <.a(
      ^.className := "btn btn-default btn-xs",
      ^.role := "button",
      <.i(^.className := "fa fa-pencil fa-fw"),
      i18n("facto.edit"))
  }

  // **************** Private inner types ****************//
  private case class Props(includeHiddenReservoirs: Boolean, router: RouterCtl[Page])
}
