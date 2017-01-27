package components

import scala.collection.immutable.Seq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spatutorial.client.spacomponents.Bootstrap.{Button, CommonStyle}
import spatutorial.shared._
import stores.{EntriesStore, LastNEntriesStoreFactory}
import stores.LastNEntriesStoreFactory.{LastNEntriesState, N}

import scalacss.ScalaCssReact._

final class Everything(implicit entriesStoreFactory: LastNEntriesStoreFactory) {

  object Panel {
    private case class Props(title: String, panelClasses: String)
    private val component = ReactComponentB[Props]("Panel")
      .renderPC((_, p, c) =>
        <.div(
          <.div(p.title),
          <.div(c)
        )
      ).build

    def apply(title: String, panelClasses: String = "")(children: ReactNode*): ReactElement = {
      component(Props(title, panelClasses), children: _*)
    }
  }

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
