package app.flux.stores

import app.flux.stores.CollapsedExpandedStateStoreFactory.State
import hydro.flux.stores.StateStore
import hydro.flux.stores.StoreFactory

final class CollapsedExpandedStateStoreFactory() extends StoreFactory {

  // **************** Public API **************** //
  def get(viewName: String): Store = getCachedOrCreate(viewName)

  // **************** Implementation of base class methods and types **************** //
  /* override */
  protected type Input = String

  protected override def createNew(input: Input): Store = new Store(input)

  /* override */
  final class Store(viewName: String) extends StateStore[State] {

    // **************** Private fields **************** //
    private var inMemoryState: State = State(tableNameToExpandedMap = Map(), defaultExpanded = true)

    // **************** Public API **************** //
    def initializedWithDefaultExpanded(expanded: Boolean): Store = {
      updateState(_ => State(tableNameToExpandedMap = Map(), defaultExpanded = expanded))
      this
    }

    def setExpanded(tableName: String, expanded: Boolean): Unit = {
      updateState(s => s.copy(tableNameToExpandedMap = s.tableNameToExpandedMap.updated(tableName, expanded)))
    }

    def setExpandedForAllTables(expanded: Boolean): Unit = {
      updateState(_ => State(tableNameToExpandedMap = Map(), defaultExpanded = expanded))
    }

    // **************** Implementation of base class methods **************** //
    override def state: State = inMemoryState

    // **************** Private helper methods **************** //
    private def updateState(updateFunc: State => State): Unit = {
      val oldState = inMemoryState
      inMemoryState = updateFunc(inMemoryState)

      if (inMemoryState != oldState) {
        invokeStateUpdateListeners()
      }
    }
  }

}

object CollapsedExpandedStateStoreFactory {
  case class State(
      private[CollapsedExpandedStateStoreFactory] val tableNameToExpandedMap: Map[String, Boolean],
      private[CollapsedExpandedStateStoreFactory] val defaultExpanded: Boolean,
  ) {
    def expanded(tableName: String): Boolean = tableNameToExpandedMap.getOrElse(tableName, defaultExpanded)
  }
}
