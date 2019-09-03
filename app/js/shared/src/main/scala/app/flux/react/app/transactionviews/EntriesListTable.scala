package app.flux.react.app.transactionviews

import app.flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
import app.flux.stores.entries.WithIsPending
import app.flux.stores.entries.factories.EntriesListStoreFactory
import app.flux.stores.CollapsedExpandedStateStoreFactory
import hydro.common.I18n
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.uielements.Table
import hydro.flux.react.uielements.Table.TableRowData
import hydro.flux.react.HydroReactComponent
import hydro.flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.VdomElement
import org.scalajs.dom.console

import scala.collection.immutable.Seq
import scala.scalajs.js

private[transactionviews] final class EntriesListTable[Entry, AdditionalInput](
    implicit protected val entriesStoreFactory: EntriesListStoreFactory[Entry, AdditionalInput],
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(tableTitle: String,
            tableClasses: Seq[String] = Seq(),
            key: String = "",
            numEntriesStrategy: NumEntriesStrategy,
            collapsedExpandedStateStore: Option[CollapsedExpandedStateStoreFactory.Store] = None,
            additionalInput: AdditionalInput,
            latestEntryToTableTitleExtra: Entry => String = null,
            hideEmptyTable: Boolean = false,
            tableHeaders: Seq[VdomElement],
            calculateTableData: Entry => Seq[VdomElement]): VdomElement = {
    withRowNumber(
      tableTitle = tableTitle,
      tableClasses = tableClasses,
      key = key,
      numEntriesStrategy = numEntriesStrategy,
      collapsedExpandedStateStore = collapsedExpandedStateStore,
      additionalInput = additionalInput,
      latestEntryToTableTitleExtra = latestEntryToTableTitleExtra,
      hideEmptyTable = hideEmptyTable,
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
                    collapsedExpandedStateStore: Option[CollapsedExpandedStateStoreFactory.Store],
                    additionalInput: AdditionalInput,
                    latestEntryToTableTitleExtra: Entry => String = null,
                    hideEmptyTable: Boolean = false,
                    tableHeaders: Seq[VdomElement],
                    calculateTableDataFromEntryAndRowNum: (Entry, Int) => Seq[VdomElement]): VdomElement = {
    component
      .withKey(key)
      .apply(
        Props(
          tableTitle,
          tableClasses,
          numEntriesStrategy,
          collapsedExpandedStateStore,
          latestEntryToTableTitleExtra = Option(latestEntryToTableTitleExtra),
          hideEmptyTable,
          tableHeaders,
          calculateTableDataFromEntryAndRowNum,
          additionalInput
        ))
      .vdomElement
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependencyFromProps(
      props =>
        StateStoresDependency(
          props.collapsedExpandedStateStoreOrDummy,
          _.copy(expanded = props.collapsedExpandedStateStoreOrDummy.state.expanded)
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(
      tableTitle: String,
      tableClasses: Seq[String],
      numEntriesStrategy: NumEntriesStrategy,
      collapsedExpandedStateStore: Option[CollapsedExpandedStateStoreFactory.Store],
      latestEntryToTableTitleExtra: Option[Entry => String],
      hideEmptyTable: Boolean,
      tableHeaders: Seq[VdomElement],
      calculateTableDataFromEntryAndRowNum: (Entry, Int) => Seq[VdomElement],
      additionalInput: AdditionalInput,
  ) {
    def collapsedExpandedStateStoreOrDummy: StateStore[CollapsedExpandedStateStoreFactory.State] =
      collapsedExpandedStateStore getOrElse
        StateStore.alwaysReturning(CollapsedExpandedStateStoreFactory.State(expanded = true))
  }

  protected case class State(
      storeState: Option[entriesStoreFactory.State] = None,
      maxNumEntries: Int = 0,
      expanded: Boolean = true,
  ) {
    def withEntriesFrom(store: entriesStoreFactory.Store): State =
      copy(storeState = store.state)
  }

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with WillMount
      with WillUnmount {
    private var entriesStore: entriesStoreFactory.Store = _

    override def willMount(props: Props, state: State): Callback = LogExceptionsCallback {
      val maxNumEntries = props.numEntriesStrategy.start

      entriesStore = entriesStoreFactory.get(
        entriesStoreFactory.Input(maxNumEntries = maxNumEntries, props.additionalInput))
      entriesStore.register(EntriesStoreListener)

      $.modState(state =>
        logExceptions {
          state
            .withEntriesFrom(entriesStore)
            .copy(maxNumEntries = maxNumEntries)
      }).runNow()
    }

    override def willUnmount(props: Props, state: State): Callback = LogExceptionsCallback {
      entriesStore.deregister(EntriesStoreListener)
      entriesStore = null
    }

    override def render(props: Props, state: State): VdomElement = logExceptions {
      if (props.hideEmptyTable && state.storeState.isDefined && state.storeState.get.isEmpty) {
        <.span()
      } else {
        Table(
          title = props.tableTitle,
          tableClasses = props.tableClasses,
          expanded = state.expanded,
          onToggleCollapsedExpanded =
            props.collapsedExpandedStateStore map { store => () =>
              store.setExpandedForSingleTable(!state.expanded)
            },
          expandNumEntriesCallback = {
            if (state.storeState.isDefined && state.storeState.get.hasMore) {
              Some(expandMaxNumEntries(props, state))
            } else { None }
          },
          tableTitleExtra = tableTitleExtra(props, state),
          tableHeaders = props.tableHeaders,
          tableRowDatas = state.storeState match {
            case Some(storeState) =>
              storeState.entries.reverse.zipWithIndex.map {
                case (WithIsPending(entry, isPending), index) =>
                  TableRowData(
                    props.calculateTableDataFromEntryAndRowNum(entry, index),
                    deemphasize = isPending)
              }
            case None =>
              for (i <- 0 until state.maxNumEntries + 1) yield {
                TableRowData(
                  Seq[VdomElement](
                    <.td(
                      ^.colSpan := props.tableHeaders.size,
                      ^.style := js.Dictionary("color" -> "white"),
                      "...")),
                  deemphasize = false)
              }
          }
        )
      }
    }

    def tableTitleExtra(props: Props, state: State): VdomElement = {
      state.storeState match {
        case Some(storeState) =>
          val numEntries = storeState.entries.size + (if (storeState.hasMore) "+" else "")
          <.span(
            <<.ifDefined(props.latestEntryToTableTitleExtra) { latestEntryToTableTitleExtra =>
              <<.ifDefined(storeState.entries.lastOption) { latestEntry =>
                <.span(latestEntryToTableTitleExtra(latestEntry.entry), " ")
              }
            },
            <.span(^.style := js.Dictionary("color" -> "#999"), s"(${i18n("app.n-entries", numEntries)})")
          )
        case None =>
          Bootstrap.FontAwesomeIcon("circle-o-notch", spin = true)
      }
    }

    private def expandMaxNumEntries(props: Props, state: State): Callback = LogExceptionsCallback {
      def updateMaxNumEntries(maxNumEntries: Int): Unit = {
        entriesStore.deregister(EntriesStoreListener)
        entriesStore = entriesStoreFactory.get(
          entriesStoreFactory.Input(maxNumEntries = maxNumEntries, props.additionalInput))
        entriesStore.register(EntriesStoreListener)
        $.modState(state =>
          logExceptions(state.withEntriesFrom(entriesStore).copy(maxNumEntries = maxNumEntries))).runNow()
      }

      val nextMaxNumEntries = {
        val nextNCandidates = props.numEntriesStrategy.intermediateBeforeInf :+ Int.MaxValue
        nextNCandidates.filter(_ > state.maxNumEntries).head
      }
      console.log(s"  Expanding #storeState from ${state.maxNumEntries} to $nextMaxNumEntries")
      updateMaxNumEntries(maxNumEntries = nextMaxNumEntries)
    }

    object EntriesStoreListener extends StateStore.Listener {
      override def onStateUpdate() = {
        $.modState(state => logExceptions(state.withEntriesFrom(entriesStore))).runNow()
      }
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
