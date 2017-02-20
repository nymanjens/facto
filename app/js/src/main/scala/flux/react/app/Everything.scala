package flux.react.app

import common.I18n
import common.Formatting._
import common.time.Clock
import flux.react.app.Everything.NumEntriesStrategy
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

  private class Backend($: BackendScope[Everything.Props, Everything.State]) extends EntriesStore.Listener {
    private var entriesStore: EntriesStore[LastNEntriesState] = null

    def willMount(state: Everything.State): Callback = Callback{
      entriesStore = entriesStoreFactory.get(N(state.n))
      entriesStore.register(this)
      $.modState(state => state.withEntriesStateFrom(entriesStore)).runNow()
    }

    def willUnmount(): Callback = Callback {
      entriesStore.deregister(this)
      entriesStore = null
    }

    override def onStateUpdate() = {
      $.modState(state => state.withEntriesStateFrom(entriesStore)).runNow()
    }

    def render(props: Everything.Props, state: Everything.State) = {
      uielements.Panel(i18n("facto.genral-information-about-all-entries"))(
        uielements.Table(
          title = i18n("facto.all"),
          tableClasses = Seq("table-everything"),
          moreEntriesCallback =
            if (state.entriesState.hasMore) Some(expandNumEntries(state)) else None,
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
          tableDatas = state.entriesState.entries.reverse.map(entry =>
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

    private def expandNumEntries(state: Everything.State): Callback = Callback{
      def updateN(n: Int) = {
        entriesStore.deregister(this)
        entriesStore = entriesStoreFactory.get(N(n))
        entriesStore.register(this)
        println(s"Done registering")
        println(s"state.hasMore = ${entriesStore.state.hasMore}")
        $.modState(state => state.withEntriesStateFrom(entriesStore).copy(n = n)).runNow()
      }

      val nextN = {
        val nextNCandidates = numEntriesStrategy.intermediateBeforeInf :+ Int.MaxValue
        nextNCandidates.filter(_ > state.n).head
      }
        println(s"  Expanding #entries from ${state.n} to $nextN")
      updateN(n =nextN )
    }

  }

  private val component = ReactComponentB[Everything.Props]("Everything")
    .initialState(Everything.State(LastNEntriesState.empty, n = numEntriesStrategy.start))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  def apply(): ReactElement = {
    component(Everything.Props())
  }

  protected def numEntriesStrategy: NumEntriesStrategy = NumEntriesStrategy(
    start = 5,
    intermediateBeforeInf = Seq(30))
}

object Everything {
  private case class Props()

  private case class State(entriesState: LastNEntriesState, n: Int) {
    def withEntriesStateFrom(store: EntriesStore[LastNEntriesState]): State =
      copy(entriesState = store.state)
  }

  case class NumEntriesStrategy(start: Int, intermediateBeforeInf: Seq[Int] = Seq()) {
    // Argument validation
    if (intermediateBeforeInf.nonEmpty) {
      val seq = start +: intermediateBeforeInf
      for ((prev, next) <- seq.dropRight(1) zip seq.drop(1)) {
        require(prev < next, s"$prev should be strictly smaller than $next")
      }
    }
  }
}
