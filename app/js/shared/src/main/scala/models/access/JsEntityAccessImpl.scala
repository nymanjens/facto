package models.access

import common.LoggingUtils.logExceptions
import models.Entity
import models.access.JsEntityAccess.Listener
import models.modification.{EntityModification, EntityType}
import models.user.User

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[access] final class JsEntityAccessImpl(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy,
    entityModificationPushClientFactory: EntityModificationPushClientFactory)
    extends JsEntityAccess {

  private var listeners: Seq[Listener] = Seq()
  private var _pendingModifications: PendingModifications =
    PendingModifications(Seq(), persistedLocally = false)
  private var isCallingListeners: Boolean = false
  private val queryBlockingFutures: mutable.Buffer[Future[Unit]] = mutable.Buffer()

  // Attach events to local database loading
  async {
    await(remoteDatabaseProxy.localDatabaseReadyFuture)
    val existingPendingModifications = await(remoteDatabaseProxy.pendingModifications())

    _pendingModifications = _pendingModifications.copy(persistedLocally = true)

    // Heuristic: When the local database is also loaded and the pending modifications are loaded, pending
    // modifications will be stored or at least start being stored
    invokeListenersAsync(_.pendingModificationsPersistedLocally())

    if (existingPendingModifications.nonEmpty) {
      await(persistModifications(existingPendingModifications))
    }

    // Send pending modifications whenever connection with the server is restored
    entityModificationPushClientFactory.pushClientsAreOnline.deregisterListener { isOnline =>
      if (isOnline) {
        if (_pendingModifications.modifications.nonEmpty) {
          persistModifications(_pendingModifications.modifications)
        }
      }
    }
  }

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E] = {
    DbResultSet.fromExecutor(new DbQueryExecutor.Async[E] {
      override def data(dbQuery: DbQuery[E]) = async {
        if (queryBlockingFutures.nonEmpty) {
          await(queryBlockingFutures.last)
        }
        await(remoteDatabaseProxy.queryExecutor[E]().data(dbQuery))
      }
      override def count(dbQuery: DbQuery[E]) = async {
        if (queryBlockingFutures.nonEmpty) {
          await(queryBlockingFutures.last)
        }
        await(remoteDatabaseProxy.queryExecutor[E]().count(dbQuery))
      }
    })
  }

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))

  override def pendingModifications = _pendingModifications

  // **************** Setters ****************//
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = logExceptions {
    require(!isCallingListeners)

    _pendingModifications ++= modifications

    val listenersInvoked = invokeListenersAsync(_.modificationsAddedOrPendingStateChanged(modifications))

    val persistResponse = remoteDatabaseProxy.persistEntityModifications(modifications)

    val queryBlockingFuture = persistResponse.queryReflectsModificationsFuture
    queryBlockingFutures += queryBlockingFuture
    queryBlockingFuture map { _ =>
      queryBlockingFutures -= queryBlockingFuture
    }

    async {
      await(persistResponse.completelyDoneFuture)
      await(listenersInvoked)
    }
  }

  override def clearLocalDatabase(): Future[Unit] = {
    remoteDatabaseProxy.clearLocalDatabase()
  }

  // **************** Other ****************//
  override def registerListener(listener: Listener): Unit = {
    require(!isCallingListeners)

    listeners = listeners :+ listener
  }

  override private[access] def startCheckingForModifiedEntityUpdates(): Unit = {
    remoteDatabaseProxy.startCheckingForModifiedEntityUpdates(modifications => {
      _pendingModifications --= modifications
      invokeListenersAsync(_.modificationsAddedOrPendingStateChanged(modifications))
    })
  }

  // **************** Private helper methods ****************//
  private def invokeListenersAsync(func: Listener => Unit): Future[Unit] = {
    Future {
      logExceptions {
        require(!isCallingListeners)
        isCallingListeners = true
        listeners.foreach(func)
        isCallingListeners = false
      }
    }
  }
}
