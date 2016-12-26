package stores

import models.access.{EntityModification, RemoteDatabaseProxy}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] abstract class EntriesStore(implicit database: RemoteDatabaseProxy) {
  type State

  database.registerListener(RemoteDatabaseProxyListener)

  private var _state: Option[State] = None
  private var stateUpdateListeners: Seq[EntitiesStoreListener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: State = {
    if (stateUpdateListeners.isEmpty) {
      updateState()
    }

    _state.get
  }

  final def register(listener: EntitiesStoreListener): Unit = {
    require(!isCallingListeners)
    if (stateUpdateListeners.isEmpty) {
      // Update state since it might have become outdated while there were no listeners
      updateState()
    }
    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  final def deregister(listener: EntitiesStoreListener): Unit = {
    require(!isCallingListeners)
    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): State

  protected def modificationImpactsState(entityModification: EntityModification): Boolean

  // **************** Private helper methods ****************//
  private def updateState(): Unit = {
    _state = Some(calculateState())
  }

  private def impactsState(modifications: Seq[EntityModification]): Boolean = {
    !modifications.toStream.filter(modificationImpactsState _).take(1).isEmpty
  }

  private def invokeListenersAsync(): Unit = {
    Future {
      require(!isCallingListeners)
      isCallingListeners = true
      stateUpdateListeners.foreach(_.onStateUpdate())
      isCallingListeners = false
    }
  }

  // **************** Inner type definitions ****************//
  trait EntitiesStoreListener {
    def onStateUpdate(): Unit
  }

  private object RemoteDatabaseProxyListener extends RemoteDatabaseProxy.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)
      if (!stateUpdateListeners.isEmpty) {
        if (impactsState(modifications)) {
          updateState()
          invokeListenersAsync()
        }
      }
    }

    override def persistedRemotely(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)
      if (!stateUpdateListeners.isEmpty) {
        if (impactsState(modifications)) {
          invokeListenersAsync()
        }
      }
    }
    override def loadedDatabase(): Unit = {
      require(!isCallingListeners)
      if (!stateUpdateListeners.isEmpty) {
        updateState()
        invokeListenersAsync()
      }
    }
  }
}
