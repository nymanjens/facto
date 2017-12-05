package flux.react.app.transactionviews

import common.I18n
import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import flux.react.uielements
import flux.stores.entries.{EntriesListStoreFactory, EntriesStore}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^.VdomElement

import scala.collection.immutable.Seq

private[transactionviews] final class EntriesListTable[Entry, AdditionalInput](
    implicit entriesStoreFactory: EntriesListStoreFactory[Entry, AdditionalInput],
    i18n: I18n) {

  private val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialStateFromProps(props =>
      State(EntriesListStoreFactory.State.empty, maxNumEntries = props.numEntriesStrategy.start))
    .renderBackend[Backend]
    .componentWillMount(scope => scope.backend.willMount(scope.props, scope.state))
    .componentWillUnmount(scope => scope.backend.willUnmount())
    .build

  // **************** API ****************//
  def apply(tableTitle: String,
            tableClasses: Seq[String] = Seq(),
            key: String = "",
            numEntriesStrategy: NumEntriesStrategy,
            additionalInput: AdditionalInput,
            tableHeaders: Seq[VdomElement],
            calculateTableData: Entry => Seq[VdomElement]): VdomElement = {
    withRowNumber(
      tableTitle = tableTitle,
      tableClasses = tableClasses,
      key = key,
      numEntriesStrategy = numEntriesStrategy,
      additionalInput = additionalInput,
      tableHeaders = tableHeaders,
      calculateTableDataFromEntryAndRowNum = (entry, rowNum) => calculateTableData(entry)
    )
  }

  /**
    * @param calculateTableDataFromEntryAndRowNum Returns an a seq of table datas from an entry and the number of the
    *                                             row. The first row is zero.
    */
  def withRowNumber(tableTitle: String,
                    tableClasses: Seq[String] = Seq(),
                    key: String = "",
                    numEntriesStrategy: NumEntriesStrategy,
                    additionalInput: AdditionalInput,
                    tableHeaders: Seq[VdomElement],
                    calculateTableDataFromEntryAndRowNum: (Entry, Int) => Seq[VdomElement]): VdomElement = {
    component
      .withKey(key)
      .apply(
        Props(
          tableTitle,
          tableClasses,
          numEntriesStrategy,
          tableHeaders,
          calculateTableDataFromEntryAndRowNum,
          additionalInput))
      .vdomElement
  }

  // **************** Private types ****************//
  private case class Props(tableTitle: String,
                           tableClasses: Seq[String],
                           numEntriesStrategy: NumEntriesStrategy,
                           tableHeaders: Seq[VdomElement],
                           calculateTableDataFromEntryAndRowNum: (Entry, Int) => Seq[VdomElement],
                           additionalInput: AdditionalInput)

  private case class State(entries: entriesStoreFactory.State, maxNumEntries: Int) {
    def withEntriesFrom(store: entriesStoreFactory.Store): State =
      copy(entries = store.state)
  }

  private class Backend($ : BackendScope[Props, State]) extends EntriesStore.Listener {
    private var entriesStore: entriesStoreFactory.Store = _

    def willMount(props: Props, state: State): Callback = LogExceptionsCallback {
      entriesStore = entriesStoreFactory.get(
        entriesStoreFactory.Input(maxNumEntries = state.maxNumEntries, props.additionalInput))
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
        title = props.tableTitle,
        tableClasses = props.tableClasses,
        expandNumEntriesCallback =
          if (state.entries.hasMore) Some(expandMaxNumEntries(props, state)) else None,
        tableHeaders = props.tableHeaders,
        tableDatas = state.entries.entries.reverse.zipWithIndex.map {
          case (entry, index) =>
            props.calculateTableDataFromEntryAndRowNum(entry, index)
        }
      )
    }

    private def expandMaxNumEntries(props: Props, state: State): Callback = LogExceptionsCallback {
      def updateMaxNumEntries(maxNumEntries: Int): Unit = {
        entriesStore.deregister(this)
        entriesStore = entriesStoreFactory.get(
          entriesStoreFactory.Input(maxNumEntries = maxNumEntries, props.additionalInput))
        entriesStore.register(this)
        $.modState(state =>
          logExceptions(state.withEntriesFrom(entriesStore).copy(maxNumEntries = maxNumEntries))).runNow()
      }

      val nextMaxNumEntries = {
        val nextNCandidates = props.numEntriesStrategy.intermediateBeforeInf :+ Int.MaxValue
        nextNCandidates.filter(_ > state.maxNumEntries).head
      }
      println(s"  Expanding #entries from ${state.maxNumEntries} to $nextMaxNumEntries")
      updateMaxNumEntries(maxNumEntries = nextMaxNumEntries)
    }

  }
}

private[transactionviews] object EntriesListTable {

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
