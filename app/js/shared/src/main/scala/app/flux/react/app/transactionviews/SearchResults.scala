package app.flux.react.app.transactionviews

import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.uielements.Panel
import common.Formatting._
import common.I18n
import common.money.ExchangeRateManager
import hydro.common.time.Clock
import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import hydro.flux.router.RouterContext
import app.flux.react.uielements
import app.flux.stores.entries.GeneralEntry
import app.flux.stores.entries.factories.ComplexQueryStoreFactory
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import app.models.access.EntityAccess
import app.models.accounting.config.Config

import scala.collection.immutable.Seq

final class SearchResults(implicit complexQueryStoreFactory: ComplexQueryStoreFactory,
                          entityAccess: EntityAccess,
                          clock: Clock,
                          accountingConfig: Config,
                          exchangeRateManager: ExchangeRateManager,
                          i18n: I18n,
                          pageHeader: PageHeader,
) {

  private val entriesListTable: EntriesListTable[GeneralEntry, ComplexQueryStoreFactory.Query] =
    new EntriesListTable

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .renderP(
      (_, props) => {
        implicit val router = props.router
        <.span(
          pageHeader(router.currentPage),
          Panel(i18n("app.search-results"))(
            entriesListTable(
              tableTitle = i18n("app.all"),
              tableClasses = Seq("table-search"),
              numEntriesStrategy = NumEntriesStrategy(start = 500),
              additionalInput = props.query,
              tableHeaders = Seq(
                <.th(i18n("app.issuer")),
                <.th(i18n("app.payed")),
                <.th(i18n("app.consumed")),
                <.th(i18n("app.beneficiary")),
                <.th(i18n("app.payed-with-to")),
                <.th(i18n("app.category")),
                <.th(i18n("app.description")),
                <.th(i18n("app.flow")),
                <.th("")
              ),
              calculateTableData = entry =>
                Seq[VdomElement](
                  <.td(entry.issuer.name),
                  <.td(entry.transactionDates.map(formatDate).mkString(", ")),
                  <.td(entry.consumedDates.map(formatDate).mkString(", ")),
                  <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
                  <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
                  <.td(entry.categories.map(_.name).mkString(", ")),
                  <.td(uielements.DescriptionWithEntryCount(entry)),
                  <.td(uielements.MoneyWithCurrency(entry.flow)),
                  <.td(uielements.TransactionGroupEditButton(entry.groupId))
              )
            )
          )
        )
      }
    )
    .build

  // **************** API ****************//
  def apply(query: String, router: RouterContext): VdomElement = {
    component(Props(query, router))
  }

  // **************** Private inner types ****************//
  private case class Props(query: String, router: RouterContext)
}
