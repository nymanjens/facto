//package flux.react.app.transactionviews
//
//import common.I18n
//import common.LoggingUtils.{LogExceptionsCallback, logExceptions}
//import common.time.Clock
//import flux.react.app.transactionviews.EntriesListTable.NumEntriesStrategy
//import flux.react.router.RouterContext
//import flux.react.uielements
//import flux.stores.entries.{
//  EntriesListStoreFactory,
//  EntriesStore,
//  SummaryForYearStoreFactory,
//  SummaryYearsStoreFactory
//}
//import japgolly.scalajs.react._
//import japgolly.scalajs.react.extra.router.Path
//import japgolly.scalajs.react.vdom.html_<^.VdomElement
//import models.accounting.TransactionGroup
//import models.accounting.config.{Account, Config}
//import models.accounting.money.ExchangeRateManager
//import models.{EntityAccess, User}
//
//import scala.collection.immutable.Seq
//
//private[transactionviews] final class SummaryTable(
//    implicit summaryForYearStoreFactory: SummaryForYearStoreFactory,
//    summaryYearsStoreFactory: SummaryYearsStoreFactory,
//    entityAccess: EntityAccess,
//    user: User,
//    clock: Clock,
//    accountingConfig: Config,
//    exchangeRateManager: ExchangeRateManager,
//    i18n: I18n) {
//
//  private val component = {
//    ScalaComponent
//      .builder[Props](getClass.getSimpleName)
//      .initialState(State())
//      .renderBackend[Backend]
//      .componentWillMount(scope => scope.backend.willMount(scope.state))
//      .componentWillUnmount(scope => scope.backend.willUnmount())
//      .componentWillReceiveProps(scope =>
//        scope.backend.willReceiveProps(scope.currentProps, scope.nextProps, scope.state))
//      .build
//  }
//
//  // **************** API ****************//
//  def apply(account: Account, query: String, hideColumnsOlderThanYear: Int, expandedYear: Int)(
//      implicit router: RouterContext): VdomElement = {
//    component(props).vdomElement
//  }
//
//  // **************** Private types ****************//
//
//  private case class Props(account: Account,
//                           query: String,
//                           hideColumnsOlderThanYear: Int,
//                           expandedYear: Int,
//                           router: RouterContext)
//
//  private case class State(entries: entriesStoreFactory.State) {
//    def withEntriesFrom(store: entriesStoreFactory.Store): State =
//      copy(entries = store.state)
//  }
//
//  private class Backend($ : BackendScope[Props, State]) extends EntriesStore.Listener {
//    private var entriesStore: entriesStoreFactory.Store = null
//
//    def willMount(state: State): Callback = LogExceptionsCallback {
//      entriesStore =
//        entriesStoreFactory.get(entriesStoreFactory.Input(maxNumEntries = state.maxNumEntries, props))
//      entriesStore.register(this)
//      $.modState(state => logExceptions(state.withEntriesFrom(entriesStore))).runNow()
//    }
//
//    def willUnmount(): Callback = LogExceptionsCallback {
//      entriesStore.deregister(this)
//      entriesStore = null
//    }
//
//    def willReceiveProps(currentProps: Props, nextProps: Props, state: State) = ???
//
//    override def onStateUpdate() = {
//      $.modState(state => logExceptions(state.withEntriesFrom(entriesStore))).runNow()
//    }
//
//    def render(props: Props, state: State) = logExceptions {
//      <.div(props.toString)
//    }
//  }
//}
