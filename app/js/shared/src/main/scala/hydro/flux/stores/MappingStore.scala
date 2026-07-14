package hydro.flux.stores

private class MappingStore[InputState, OutputState](
    inputStore: StateStore[InputState],
    mapFunction: InputState => OutputState,
) extends StateStore[OutputState] {
  onStateUpdateListenersChange()

  override final def state: OutputState = {
    mapFunction(inputStore.state)
  }

  override final protected def onStateUpdateListenersChange(): Unit = {
    if (this.stateUpdateListeners.isEmpty) {
      inputStore.deregister(InputStoreListener)
    } else {
      if (!(inputStore.stateUpdateListeners contains InputStoreListener)) {
        inputStore.register(InputStoreListener)
      }
    }
  }

  private object InputStoreListener extends StateStore.Listener {
    override def onStateUpdate(): Unit = {
      MappingStore.this.invokeStateUpdateListeners()
    }
  }
}
object MappingStore {
  def map[InputState, OutputState](
      inputStore: StateStore[InputState],
      mapFunction: InputState => OutputState,
  ): StateStore[OutputState] = {
    return new MappingStore(inputStore, mapFunction)
  }
}
