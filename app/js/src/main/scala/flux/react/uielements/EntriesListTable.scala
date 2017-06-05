package flux.react.uielements

import common.LoggingUtils.{logExceptions, LogExceptionsCallback}
import common.{I18n, LoggingUtils}
import flux.react.uielements
import flux.react.uielements.EntriesListTable.NumEntriesStrategy
import flux.stores.{EntriesStore, EntriesListStoreFactory}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import japgolly.scalajs.react.vdom._

import scala.collection.immutable.Seq

private final class EntriesListTable[Entry, Props](
    tableTitle: String,
    tableClasses: Seq[String],
    numEntriesStrategy: NumEntriesStrategy,
    tableHeaders: Seq[VdomElement],
    calculateTableData: Entry => Seq[VdomElement],
    props: Props)(implicit entriesStoreFactory: EntriesListStoreFactory[Entry, Props], i18n: I18n) {

  private val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .initialState(State(EntriesListStoreFactory.State.empty, maxNumEntries = numEntriesStrategy.start))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(): VdomElement = {
    component(props)
  }

  // **************** Private types ****************//
  private case class State(entries: entriesStoreFactory.State, maxNumEntries: Int) {
    def withEntriesFrom(store: entriesStoreFactory.Store): State =
      copy(entries = store.state)
  }

  private class Backend($ : BackendScope[Props, State]) extends EntriesStore.Listener {
    private var entriesStore: entriesStoreFactory.Store = null

    def willMount(state: State): Callback = LogExceptionsCallback {
      entriesStore =
        entriesStoreFactory.get(entriesStoreFactory.Input(maxNumEntries = state.maxNumEntries, props))
      entriesStore.register(this)
      $.modState(state => logExceptions(state.withEntriesFrom(entriesStore))).runNow()
    }

    def willUnmount(): Callback = LogExceptionsCallback {
      entriesStore.deregister(this)
      entriesStore = null
    }

    override def onStateUpdate() = {
      $.modState(state => logExceptions(state.withEntriesFrom(entriesStore))).runNow()
    }

    def render(props: Props, state: State) = logExceptions {
      uielements.Table(
        title = tableTitle,
        tableClasses = tableClasses,
        expandNumEntriesCallback = if (state.entries.hasMore) Some(expandMaxNumEntries(state)) else None,
        tableHeaders = tableHeaders,
        tableDatas = state.entries.entries.reverse.map(calculateTableData)
      )
    }

    private def expandMaxNumEntries(state: State): Callback = LogExceptionsCallback {
      def updateMaxNumEntries(maxNumEntries: Int) = {
        entriesStore.deregister(this)
        entriesStore =
          entriesStoreFactory.get(entriesStoreFactory.Input(maxNumEntries = maxNumEntries, props))
        entriesStore.register(this)
        $.modState(state =>
          logExceptions(state.withEntriesFrom(entriesStore).copy(maxNumEntries = maxNumEntries))).runNow()
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
                          tableHeaders: Seq[VdomElement],
                          calculateTableData: Entry => Seq[VdomElement],
                          props: Props = (): Unit)(
      implicit entriesStoreFactory: EntriesListStoreFactory[Entry, Props],
      i18n: I18n): VdomElement = {
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
