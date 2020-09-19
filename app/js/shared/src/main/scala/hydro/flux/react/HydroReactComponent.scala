package hydro.flux.react

import hydro.common.GuavaReplacement.Preconditions
import hydro.common.GuavaReplacement.Preconditions.checkNotNull
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.common.JsLoggingUtils.logExceptions
import hydro.flux.stores.StateStore
import japgolly.scalajs.react._
import japgolly.scalajs.react.component.builder.Builder
import japgolly.scalajs.react.vdom.html_<^._

import scala.collection.immutable.Seq

abstract class HydroReactComponent {

  // **************** Protected types to be overridden ****************//
  protected type Props
  protected type State
  protected type Backend <: BackendBase

  // **************** Protected methods to be overridden ****************//
  protected val config: ComponentConfig

  // **************** Protected final methods ****************//
  protected lazy val component = {
    val step1: Builder.Step1[Props] = ScalaComponent.builder[Props](config.componentName)
    val step2: Builder.Step2[Props, State] =
      if (this.isInstanceOf[HydroReactComponent.Stateless])
        step1.stateless.asInstanceOf[Builder.Step2[Props, State]]
      else step1.initialStateFromProps[State](config.initialStateFromProps)
    val step3: Builder.Step3[Props, State, Backend] = step2.backend(config.backendConstructor)
    var step4: Builder.Step4[Props, Children.None, State, Backend] =
      step3.renderPS((scope, props, state) => scope.backend.render(props, state))

    val dummyBackend = config.backendConstructor(null)
    if (dummyBackend.isInstanceOf[WillMount]) {
      step4 = step4
        .componentWillMount(scope =>
          scope.backend.asInstanceOf[WillMount].willMount(scope.props, scope.state)
        )
    }
    if (dummyBackend.isInstanceOf[WillUnmount]) {
      step4 = step4
        .componentWillUnmount(scope =>
          scope.backend.asInstanceOf[WillUnmount].willUnmount(scope.props, scope.state)
        )
    }
    if (dummyBackend.isInstanceOf[DidMount]) {
      step4 = step4
        .componentDidMount(scope => scope.backend.asInstanceOf[DidMount].didMount(scope.props, scope.state))
    }
    if (dummyBackend.isInstanceOf[WillReceiveProps]) {
      step4 = step4
        .componentWillReceiveProps(scope =>
          scope.backend
            .asInstanceOf[WillReceiveProps]
            .willReceiveProps(currentProps = scope.currentProps, nextProps = scope.nextProps, scope.state)
        )
    }
    if (dummyBackend.isInstanceOf[DidUpdate]) {
      step4 = step4
        .componentDidUpdate(scope =>
          scope.backend
            .asInstanceOf[DidUpdate]
            .didUpdate(
              prevProps = scope.prevProps,
              currentProps = scope.currentProps,
              prevState = scope.prevState,
              currentState = scope.currentState,
            )
        )
    }
    if (config.maybeStateStoresDependencies.nonEmpty) {
      step4 = step4
        .componentWillMount { scope =>
          logExceptions {
            for (StateStoresDependency(store, _) <- config.stateStoresDependencies(scope.props)) {
              store.register(scope.backend)
            }
            scope.backend.updateStateFromStoresCallback(scope.props)
          }
        }
        .componentWillReceiveProps { scope =>
          logExceptions {
            var anythingChanged = false
            for {
              (StateStoresDependency(oldStore, _), StateStoresDependency(newStore, _)) <-
                config.stateStoresDependencies(scope.currentProps) zip config.stateStoresDependencies(
                  scope.nextProps
                )
              if oldStore != newStore
            } {
              oldStore.deregister(scope.backend)
              newStore.register(scope.backend)
              anythingChanged = true
            }
            if (anythingChanged) {
              scope.backend.updateStateFromStoresCallback(scope.nextProps)
            } else {
              Callback.empty
            }
          }
        }
        .componentWillUnmount { scope =>
          LogExceptionsCallback {
            for (StateStoresDependency(store, _) <- config.stateStoresDependencies(scope.props)) {
              store.deregister(scope.backend)
            }
          }
        }
    }
    step4.build
  }

  // **************** Protected types ****************//
  abstract class BackendBase($ : BackendScope[Props, State]) extends StateStore.Listener {
    def render(props: Props, state: State): VdomNode

    override final def onStateUpdate() = {
      $.props.flatMap(updateStateFromStoresCallback).runNow()
    }

    private[HydroReactComponent] def updateStateFromStoresCallback(props: Props): Callback = {
      $.modState(oldState =>
        logExceptions {
          var state = oldState
          for (StateStoresDependency(_, stateUpdate) <- config.stateStoresDependencies(props)) {
            state = stateUpdate(state)
          }
          state
        }
      )
    }
  }
  trait WillMount { def willMount(props: Props, state: State): Callback }
  trait WillUnmount { def willUnmount(props: Props, state: State): Callback }
  trait DidMount { def didMount(props: Props, state: State): Callback }
  trait WillReceiveProps {
    def willReceiveProps(currentProps: Props, nextProps: Props, state: State): Callback
  }
  trait DidUpdate {
    def didUpdate(prevProps: Props, currentProps: Props, prevState: State, currentState: State): Callback
  }

  case class StateStoresDependency(store: StateStore[_], stateUpdate: State => State)
  class ComponentConfig(
      val backendConstructor: BackendScope[Props, State] => Backend,
      val initialStateFromProps: Props => State,
      val componentName: String,
      val maybeStateStoresDependencies: Option[Props => Seq[StateStoresDependency]],
  ) {
    def withStateStoresDependency(store: StateStore[_], stateUpdate: State => State): ComponentConfig =
      withStateStoresDependencyFromProps(_ => StateStoresDependency(store, stateUpdate))

    def withStateStoresDependencyFromProps(
        dependencyFromProps: Props => StateStoresDependency
    ): ComponentConfig = {
      new ComponentConfig(
        backendConstructor = backendConstructor,
        initialStateFromProps = initialStateFromProps,
        componentName = componentName,
        maybeStateStoresDependencies = Some(props => {
          stateStoresDependencies(props) :+ dependencyFromProps(props)
        }),
      )
    }

    private[HydroReactComponent] def stateStoresDependencies(props: Props): Seq[StateStoresDependency] = {
      maybeStateStoresDependencies match {
        case None                      => Seq()
        case Some(propsToDependencies) => propsToDependencies(props)
      }
    }
  }
  object ComponentConfig {
    def apply(
        backendConstructor: BackendScope[Props, State] => Backend,
        initialState: State = null.asInstanceOf[State],
        initialStateFromProps: Props => State = null,
        componentName: String = HydroReactComponent.this.getClass.getSimpleName,
        stateStoresDependencies: Option[Props => Seq[StateStoresDependency]] = None,
    ): ComponentConfig = {
      new ComponentConfig(
        backendConstructor = backendConstructor,
        initialStateFromProps =
          Option(initialState).map(s => (_: Props) => s) getOrElse checkNotNull(initialStateFromProps),
        componentName = componentName,
        maybeStateStoresDependencies = stateStoresDependencies,
      )
    }
  }
}
object HydroReactComponent {
  abstract class Stateless extends HydroReactComponent {

    protected val statelessConfig: StatelessComponentConfig

    type State = Unit
    override lazy val config = ComponentConfig(
      backendConstructor = statelessConfig.backendConstructor,
      initialState = (): Unit,
      componentName = statelessConfig.componentName,
      stateStoresDependencies = None,
    )

    case class StatelessComponentConfig(
        backendConstructor: BackendScope[Props, State] => Backend,
        componentName: String = Stateless.this.getClass.getSimpleName,
    )
  }
}
