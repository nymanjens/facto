package flux.react.uielements

import common.{I18n, LoggingUtils}
import flux.react.uielements
import flux.react.uielements.EntriesListTable.NumEntriesStrategy
import flux.stores.{EntriesStore, EntriesStoreListFactory}
import japgolly.scalajs.react._

import scala.collection.immutable.Seq

private final class EntriesListTable[Entry, Props](tableTitle: String,
                                                   tableClasses: Seq[String],
                                                   numEntriesStrategy: NumEntriesStrategy,
                                                   tableHeaders: Seq[ReactElement],
                                                   calculateTableData: Entry => Seq[ReactElement],
                                                   props: Props)(
                                                    implicit entriesStoreFactory: EntriesStoreListFactory[Entry, Props],
                                                    i18n: I18n) {

  private val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState(State(EntriesStoreListFactory.State.empty, maxNumEntries = 1))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(): ReactElement = {
    component(props)
  }

  // **************** Private types ****************//
  private case class State(entries: entriesStoreFactory.State, maxNumEntries: Int) {
    def withEntriesFrom(store: entriesStoreFactory.Store): State =
      copy(entries = store.state)
  }

  private class Backend($: BackendScope[Props, State]) extends EntriesStore.Listener {
    private var entriesStore: entriesStoreFactory.Store = null

    def willMount(state: State): Callback = Callback {
      entriesStore = entriesStoreFactory.get(entriesStoreFactory.Input(maxNumEntries = state.maxNumEntries, props))
      entriesStore.register(this)
      $.modState(state => State(
        entries = entriesStore.state,
        maxNumEntries = numEntriesStrategy.start)).runNow()
    }

    def willUnmount(): Callback = Callback {
      entriesStore.deregister(this)
      entriesStore = null
    }

    override def onStateUpdate() = {
      $.modState(state => state.withEntriesFrom(entriesStore)).runNow()
    }

    def render(props: Props, state: State) = LoggingUtils.logExceptions {
      uielements.Table(
        title = tableTitle,
        tableClasses = tableClasses,
        expandNumEntriesCallback =
          if (state.entries.hasMore) Some(expandMaxNumEntries(state)) else None,
        tableHeaders = tableHeaders,
        tableDatas = state.entries.entries.reverse.map(calculateTableData)
      )
    }

    private def expandMaxNumEntries(state: State): Callback = Callback {
      def updateMaxNumEntries(maxNumEntries: Int) = {
        entriesStore.deregister(this)
        entriesStore = entriesStoreFactory.get(entriesStoreFactory.Input(maxNumEntries = maxNumEntries, props))
        entriesStore.register(this)
        $.modState(state => state.withEntriesFrom(entriesStore).copy(maxNumEntries = maxNumEntries)).runNow()
      }

      val nextMaxNumEntries = {
        val nextNCandidates = numEntriesStrategy.intermediateBeforeInf :+ Int.MaxValue
        nextNCandidates.filter(_ > state.maxNumEntries).head
      }
      println(s"  Expanding #entries from ${state.maxNumEntries} to $nextMaxNumEntries")
      updateMaxNumEntries(maxNumEntries = nextMaxNumEntries)
    }

  }
}

object EntriesListTable {

  def apply[Entry, Props](tableTitle: String,
                          tableClasses: Seq[String] = Seq(),
                          numEntriesStrategy: NumEntriesStrategy,
                          tableHeaders: Seq[ReactElement],
                          calculateTableData: Entry => Seq[ReactElement],
                          props: Props = (): Unit)(
                           implicit entriesStoreFactory: EntriesStoreListFactory[Entry, Props],
                           i18n: I18n): ReactElement = {
    new EntriesListTable(
      tableTitle,
      tableClasses,
      numEntriesStrategy,
      tableHeaders,
      calculateTableData,
      props).apply()
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
