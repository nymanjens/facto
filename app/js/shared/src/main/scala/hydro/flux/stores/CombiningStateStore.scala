package hydro.flux.stores

import scala.collection.immutable.Seq

abstract class CombiningStateStore[InputStateA, InputStateB, OutputState](
    storeA: StateStore[InputStateA],
    storeB: StateStore[InputStateB]
) extends StateStore[OutputState] {
  require(storeA.stateUpdateListeners.isEmpty, "Combining should happen on a newly created store")
  require(storeB.stateUpdateListeners.isEmpty, "Combining should happen on a newly created store")

  protected def combineStoreStates(storeAState: InputStateA, storeBState: InputStateB): OutputState

  override final def state: OutputState = {
    combineStoreStates(storeA.state, storeB.state)
  }

  override final protected def onStateUpdateListenersChange(): Unit = {
    for (inputStore <- Seq(storeA, storeB)) {
      if (this.stateUpdateListeners.isEmpty) {
        inputStore.deregister(InputStoreListener)
      } else {
        if (!(inputStore.stateUpdateListeners contains InputStoreListener)) {
          inputStore.register(InputStoreListener)
        }
      }
    }
  }

  private object InputStoreListener extends StateStore.Listener {
    override def onStateUpdate(): Unit = {
      CombiningStateStore.this.invokeStateUpdateListeners()
    }
  }
}
