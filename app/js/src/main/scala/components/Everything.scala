package components

import common.I18n

import scala.collection.immutable.Seq
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._
import components.ReactVdomUtils.^^
import spatutorial.client.spacomponents.Bootstrap.{Button, CommonStyle}
import spatutorial.shared._
import stores.{EntriesStore, LastNEntriesStoreFactory}
import stores.LastNEntriesStoreFactory.{LastNEntriesState, N}

import scalacss.ScalaCssReact._

object ReactVdomUtils {
  object ^^ {
    def classes(cls: String*): TagMod = classes(cls.toVector)
    def classes(cls: Seq[String]): TagMod = ^.classSetM(cls.map(c => (c, true)).toMap)
  }
}

final class Everything(implicit entriesStoreFactory: LastNEntriesStoreFactory,
                       i18n: I18n) {

  object Panel {
    private case class Props(title: String, panelClasses: Seq[String])
    private val component = ReactComponentB[Props]("Panel")
      .renderPC((_, props, children) =>
        <.div(^^.classes(Seq("row", "add-toc-level-1") ++ props.panelClasses),
          <.div(^^.classes("col-lg-12"),
            <.div(^^.classes("panel panel-default"),
              <.div(^^.classes("panel-heading toc-title"),
                props.title
              ),
              <.div(^^.classes("panel-body"),
                children
              )
            )
          )
        )
      ).build

    def apply(title: String, panelClasses: Seq[String] = Seq())(children: ReactNode*): ReactElement = {
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
