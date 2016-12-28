package stores

import models.access.RemoteDatabaseProxy
import models.manager.EntityModification

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[stores] abstract class EntriesStore[State](implicit database: RemoteDatabaseProxy) {
  database.registerListener(RemoteDatabaseProxyListener)

  private var _state: Option[State] = None
  private var stateUpdateListeners: Seq[EntriesStore.Listener] = Seq()
  private var isCallingListeners: Boolean = false

  // **************** Public API ****************//
  final def state: State = {
    if (_state.isEmpty) {
      updateState()
    }

    _state.get
  }

  final def register(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners :+ listener
  }

  final def deregister(listener: EntriesStore.Listener): Unit = {
    require(!isCallingListeners)

    stateUpdateListeners = stateUpdateListeners.filter(_ != listener)
  }

  // **************** Abstract methods ****************//
  protected def calculateState(): State

  protected def modificationImpactsState(entityModification: EntityModification, state: State): Boolean

  // **************** Private helper methods ****************//
  private def updateState(): Unit = {
    _state = Some(calculateState())
  }

  private def impactsState(modifications: Seq[EntityModification]): Boolean =
    modifications.toStream.filter(m => modificationImpactsState(m, state)).take(1).nonEmpty

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

      if (_state.isDefined) {
        if (impactsState(modifications)) {
          if (stateUpdateListeners.isEmpty) {
            _state = None
          } else {
            updateState()
            invokeListenersAsync()
          }
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

      if (stateUpdateListeners.isEmpty) {
        _state = None
      } else {
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
