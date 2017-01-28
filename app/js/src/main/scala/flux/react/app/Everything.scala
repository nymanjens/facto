package flux.react.app

import common.I18n
import flux.react.uielements._
import flux.stores.LastNEntriesStoreFactory.{LastNEntriesState, N}
import flux.stores.{EntriesStore, LastNEntriesStoreFactory}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.collection.immutable.Seq

final class Everything(implicit entriesStoreFactory: LastNEntriesStoreFactory,
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
      Panel("this is a test title")(
        <.div("inside the panel"),
        <.div(s"inside the panel 2: $state")
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
