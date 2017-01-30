package flux.react.app

import common.I18n
import common.Formatting._
import common.time.Clock
import flux.react.uielements
import flux.stores.LastNEntriesStoreFactory.{LastNEntriesState, N}
import flux.stores.{EntriesStore, LastNEntriesStoreFactory}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import models.EntityAccess
import models.accounting.config.Config
import models.accounting.money.ExchangeRateManager

import scala.collection.immutable.Seq

final class Everything(implicit entriesStoreFactory: LastNEntriesStoreFactory,
                       entityAccess: EntityAccess,
                       clock: Clock,
                       accountingConfig: Config,
                       exchangeRateManager: ExchangeRateManager,
                       i18n: I18n) {

  class Backend($: BackendScope[N, LastNEntriesState]) extends EntriesStore.Listener {
    private var entriesStore: EntriesStore[LastNEntriesState] = null

    def mounted(n: N): Callback = Callback {
      entriesStore = entriesStoreFactory.get(n)
      entriesStore.register(this)
    }

    def willUnmount(): Callback = Callback {
      entriesStore.deregister(this)
      entriesStore = null
    }

    override def onStateUpdate() = {
      $.modState(_ => entriesStore.state).runNow()
    }

    def render(n: N, state: LastNEntriesState) = {
      uielements.Panel(i18n("facto.genral-information-about-all-entries"))(
        uielements.Table(
          title = i18n("facto.all"),
          tableClasses = Seq("table-everything"),
          moreEntriesCallback = Option(Callback(println("  test!!!"))),
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
          tableDatas = state.entries.map(entry =>
            Seq[ReactElement](
              <.td(entry.issuer.name),
              <.td(entry.transactionDates.map(formatDate).mkString(", ")),
              <.td(entry.consumedDates.map(formatDate).mkString(", ")),
              <.td(entry.beneficiaries.map(_.shorterName).mkString(", ")),
              <.td(entry.moneyReservoirs.map(_.shorterName).mkString(", ")),
              <.td(entry.categories.map(_.name).mkString(", ")),
              <.td(uielements.DescriptionWithEntryCount(entry)),
              <.td(entry.flow.toHtmlWithCurrency),
              <.td(uielements.TransactionGroupEditButton(entry.groupId))
            )
          )
        )
      )
    }
  }

  private val component = ReactComponentB[N]("Everything")
    .initialState(LastNEntriesState(Seq()))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  def apply(n: Int): ReactElement = {
    component(N(n))
  }
}
