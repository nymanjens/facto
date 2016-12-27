package stores

import models.access.{EntityModification, RemoteDatabaseProxy}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] abstract class EntriesStore[State](firstListener: EntriesStore.Listener)
                                                  (implicit database: RemoteDatabaseProxy) {
  database.registerListener(RemoteDatabaseProxyListener)

  private var _state: State = calculateState(oldState = None)
  private var stateUpdateListeners: Seq[EntriesStore.Listener] = Seq(firstListener)
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: State = {
    if (stateUpdateListeners.isEmpty) {
      updateState()
    }

    _state
  }

  final def register(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)
    if (stateUpdateListeners.isEmpty) {
      // Update state since it might have become outdated while there were no listeners
      updateState()
    }
    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  final def deregister(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)
    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Abstract methods ****************//
  protected def calculateState(oldState: Option[State]): State

  protected def modificationImpactsState(entityModification: EntityModification, state: State): Boolean

  // **************** Private helper methods ****************//
  private def updateState(): Unit = {
    _state = calculateState(Some(_state))
  }

  private def impactsState(modifications: Seq[EntityModification]): Boolean =
    modifications.toStream.filter(m => modificationImpactsState(m, _state)).take(1).nonEmpty

  private def invokeListenersAsync(): Unit = {
    Future {
      require(!isCallingListeners)
      isCallingListeners = true
      stateUpdateListeners.foreach(_.onStateUpdate())
      isCallingListeners = false
    }
  }

  // **************** Inner type definitions ****************//
  private object RemoteDatabaseProxyListener extends RemoteDatabaseProxy.Listener {
    override def addedLocally(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)
      if (stateUpdateListeners.nonEmpty) {
        if (impactsState(modifications)) {
          updateState()
          invokeListenersAsync()
        }
      }
    }

    override def persistedRemotely(modifications: Seq[EntityModification]): Unit = {
      require(!isCallingListeners)
      if (stateUpdateListeners.nonEmpty) {
        if (impactsState(modifications)) {
          invokeListenersAsync()
        }
      }
    }
    override def loadedDatabase(): Unit = {
      require(!isCallingListeners)
      if (stateUpdateListeners.nonEmpty) {
        updateState()
        invokeListenersAsync()
      }
    }
  }
}

object EntriesStore {
  trait Listener {
    def onStateUpdate(): Unit
  }
}
