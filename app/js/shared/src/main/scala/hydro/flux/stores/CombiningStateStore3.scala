package hydro.flux.stores

import scala.collection.immutable.Seq

abstract class CombiningStateStore3[InputStateA, InputStateB, InputStateC, OutputState](
    storeA: StateStore[InputStateA],
    storeB: StateStore[InputStateB],
    storeC: StateStore[InputStateC],
) extends StateStore[OutputState] {
  onStateUpdateListenersChange()

  protected def combineStoreStates(
      storeAState: InputStateA,
      storeBState: InputStateB,
      storeCState: InputStateC,
  ): OutputState

  override final def state: OutputState = {
    combineStoreStates(storeA.state, storeB.state, storeC.state)
  }

  override final protected def onStateUpdateListenersChange(): Unit = {
    for (inputStore <- Seq(storeA, storeB, storeC)) {
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
      CombiningStateStore3.this.invokeStateUpdateListeners()
    }
  }
}
