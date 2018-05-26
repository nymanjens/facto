package models.access

import api.ScalaJsApi.UpdateToken
import common.LoggingUtils.logExceptions
import common.ScalaUtils.visibleForTesting
import models.Entity
import models.access.JsEntityAccess.Listener
import models.modification.{EntityModification, EntityType}
import models.user.User
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[access] final class JsEntityAccessImpl(allUsers: Seq[User])(
    implicit remoteDatabaseProxy: RemoteDatabaseProxy)
    extends JsEntityAccess {

  private var listeners: Seq[Listener] = Seq()
  private val allLocallyCreatedModifications: mutable.Set[EntityModification] = mutable.Set()
  private var _pendingModifications: PendingModifications =
    PendingModifications(Set(), persistedLocally = false)
  private var isCallingListeners: Boolean = false
  private val queryBlockingFutures: mutable.Buffer[Future[Unit]] = mutable.Buffer()

  // Attach events to local database loading
  async {
    await(remoteDatabaseProxy.localDatabaseReadyFuture)
    val existingPendingModifications = await(remoteDatabaseProxy.pendingModifications())

    _pendingModifications = _pendingModifications.copy(persistedLocally = true)
    _pendingModifications ++= existingPendingModifications

    if (existingPendingModifications.nonEmpty) {
      // Call listeners with initially present pending modifications
      invokeListenersAsync(_.modificationsAddedOrPendingStateChanged(existingPendingModifications))
    }

    // Heuristic: When the local database is also loaded and the pending modifications are loaded, pending
    // modifications will be stored or at least start being stored
    invokeListenersAsync(_.pendingModificationsPersistedLocally())
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

    allLocallyCreatedModifications ++= modifications
    _pendingModifications ++= modifications

    val listenersInvoked = invokeListenersAsync(_.modificationsAddedOrPendingStateChanged(modifications))

    val persistResponse = remoteDatabaseProxy.persistEntityModifications(modifications)

    val queryBlockingFuture = persistResponse.queryReflectsModifications
    queryBlockingFutures += queryBlockingFuture
    queryBlockingFuture map { _ =>
      queryBlockingFutures -= queryBlockingFuture
    }

    async {
      await(persistResponse.completelyDone)
      await(listenersInvoked)
    }
  }

  // **************** Other ****************//
  override def registerListener(listener: Listener): Unit = {
    require(!isCallingListeners)

    listeners = listeners :+ listener
  }

  override private[access] def startSchedulingModifiedEntityUpdates(): Unit = {
    var timeout = 5.seconds
    def cyclicLogic(updateToken: Option[UpdateToken]): Unit = async {
      val nextUpdateToken = await(updateModifiedEntities(updateToken))
      js.timers.setTimeout(timeout)(cyclicLogic(Some(nextUpdateToken)))
      timeout * 1.02
    }

    js.timers.setTimeout(0)(cyclicLogic(updateToken = None))
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

  @visibleForTesting private[access] def updateModifiedEntities(
      updateToken: Option[UpdateToken]): Future[UpdateToken] = async {
    val response = await(remoteDatabaseProxy.getAndApplyRemotelyModifiedEntities(updateToken))
    if (response.changes.nonEmpty) {
      console.log(s"  ${response.changes.size} remote modifications received")
      _pendingModifications --= response.changes
      await(invokeListenersAsync(_.modificationsAddedOrPendingStateChanged(response.changes)))
    }
    response.nextUpdateToken
  }
}
