package flux.react.app

import common.Formatting._
import common.I18n
import common.time.Clock
import flux.react.app.transactiongroupform.TotalFlowRestrictionInput.TotalFlowRestriction
import flux.react.router.Page
import flux.react.uielements
import flux.react.uielements.{EntriesListTable, MoneyWithCurrency}
import flux.react.uielements.EntriesListTable.NumEntriesStrategy
import flux.stores.AllEntriesStoreFactory
import flux.stores.entries.GeneralEntry
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import models.EntityAccess
import models.accounting.config.Config
import models.accounting.money.{Currency, ExchangeRateManager, ReferenceMoney}

import scala.collection.immutable.Seq

final class Everything(implicit entriesStoreFactory: AllEntriesStoreFactory,
                       entityAccess: EntityAccess,
                       clock: Clock,
                       accountingConfig: Config,
                       exchangeRateManager: ExchangeRateManager,
                       i18n: I18n) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .renderP(
      (_, props) =>
        uielements.Panel(i18n("facto.genral-information-about-all-entries"))(
          uielements.EntriesListTable[GeneralEntry, Unit](
            tableTitle = i18n("facto.all"),
            tableClasses = Seq("table-everything"),
            numEntriesStrategy = NumEntriesStrategy(start = 5, intermediateBeforeInf = Seq(30)),
            tableHeaders = Seq(
              <.th(i18n("facto.issuer")),
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
              Seq[ReactElement](
                <.td(entry.issuer.name),
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
      ))
    .build

  // **************** API ****************//
  def apply(router: RouterCtl[Page]): ReactElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterCtl[Page])
}
