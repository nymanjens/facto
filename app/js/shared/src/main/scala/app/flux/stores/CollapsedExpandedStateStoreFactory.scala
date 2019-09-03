package app.flux.stores

import app.flux.stores.CollapsedExpandedStateStoreFactory.State
import hydro.common.Listenable
import hydro.common.Listenable.WritableListenable
import hydro.flux.stores.StateStore
import hydro.flux.stores.StoreFactory

import scala.collection.mutable

final class CollapsedExpandedStateStoreFactory() {

  // **************** Public API **************** //
  def initializeView(viewName: String, defaultExpanded: Boolean = true): ViewHandle =
    new ViewHandle(viewName, defaultExpanded = defaultExpanded)

  final class ViewHandle(viewName: String, private var defaultExpanded: Boolean) extends StoreFactory {

    private val allStores: mutable.Set[Store] = mutable.Set()

    def getStore(tableName: String): Store = getCachedOrCreate(tableName)

    def setExpandedForAllTables(expanded: Boolean): Unit = {
      for (store <- allStores) {
        store.inMemoryState.set(State(expanded = expanded))
      }
      defaultExpanded = expanded
    }

    // **************** Implementation of base class methods and types **************** //
    /* override */
    protected type Input = String

    protected override def createNew(input: Input): Store = {
      val store = new Store(input)
      allStores.add(store)
      store
    }

    /* override */
    final class Store(viewName: String) extends CollapsedExpandedStateStoreFactory.Store {

      // **************** Private fields **************** //
      private[CollapsedExpandedStateStoreFactory] var inMemoryState: WritableListenable[State] =
        WritableListenable[State](State(expanded = defaultExpanded))
      inMemoryState.registerListener(newValue => invokeStateUpdateListeners())

      // **************** Implementation of base class methods **************** //
      override def state: State = inMemoryState.get
      override def setExpandedForSingleTable(expanded: Boolean): Unit = {
        inMemoryState.set(State(expanded = expanded))
      }
    }
  }
}

object CollapsedExpandedStateStoreFactory {
  case class State(expanded: Boolean)

  abstract class Store extends StateStore[State] {
    def setExpandedForSingleTable(expanded: Boolean): Unit
  }
}
