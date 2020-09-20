package app.flux.react.app.transactionviews

import hydro.common.Formatting._
import hydro.common.I18n
import app.common.money.ExchangeRateManager
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.react.uielements
import app.flux.react.uielements.DescriptionWithEntryCount
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.AllEntriesStoreFactory
import app.models.access.AppJsEntityAccess
import app.models.accounting.config.Config
import hydro.common.time.Clock
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

final class Everything(implicit
    entriesStoreFactory: AllEntriesStoreFactory,
    entityAccess: AppJsEntityAccess,
    clock: Clock,
    accountingConfig: Config,
    exchangeRateManager: ExchangeRateManager,
    i18n: I18n,
    pageHeader: PageHeader,
    descriptionWithEntryCount: DescriptionWithEntryCount,
) {

  private val entriesListTable: EntriesListTable[GeneralEntry, Unit] = new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP((_, props) => {
      implicit val router = props.router
      <.span(
        pageHeader(router.currentPage),
        Panel(i18n("app.genral-information-about-all-entries"))(
          entriesListTable(
            tableTitle = i18n("app.all"),
            tableClasses = Seq("table-everything"),
            numEntriesStrategy = NumEntriesStrategy(start = 400),
            additionalInput = (): Unit,
            tableHeaders = Seq(
              <.th(i18n("app.issuer")),
              <.th(i18n("app.payed")),
              <.th(i18n("app.consumed")),
              <.th(i18n("app.beneficiary")),
              <.th(i18n("app.payed-with-to")),
              <.th(i18n("app.category")),
              <.th(i18n("app.description")),
              <.th(i18n("app.flow")),
              <.th(""),
            ),
            calculateTableData = entry =>
              Seq[VdomElement](
                <.td(entry.issuer.name),
                <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                <.td(entry.categories.map(_.name).mkString(", ")),
                <.td(descriptionWithEntryCount(entry)),
                <.td(uielements.MoneyWithCurrency(entry.flow)),
                <.td(uielements.TransactionGroupEditButton(entry.groupId)),
              ),
          )
        ),
      )
    })
    .build

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Private inner types ****************//
  private case class Props(router: RouterContext)
}
