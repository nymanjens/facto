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
  private val localAddModificationIds: Map[EntityType.any, mutable.Set[Long]] =
    EntityType.values.map(t => t -> mutable.Set[Long]()).toMap
  private val allLocallyCreatedModifications: mutable.Set[EntityModification] = mutable.Set()
  private var isCallingListeners: Boolean = false
  private val writeReadyFutures: mutable.Buffer[Future[Unit]] = mutable.Buffer()

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E] = {
    DbResultSet.fromExecutor(new DbQueryExecutor.Async[E] {
      override def data(dbQuery: DbQuery[E]) = async {
        if (writeReadyFutures.nonEmpty) {
          await(writeReadyFutures.lastOption.get)
        }
        await(remoteDatabaseProxy.queryExecutor[E]().data(dbQuery))
      }
      override def count(dbQuery: DbQuery[E]) = async {
        if (writeReadyFutures.nonEmpty) {
          await(writeReadyFutures.lastOption.get)
        }
        await(remoteDatabaseProxy.queryExecutor[E]().count(dbQuery))
      }
    })
  }

  override def newQuerySyncForUser() =
    DbResultSet.fromExecutor(DbQueryExecutor.fromEntities(allUsers))

  override def hasLocalAddModifications[E <: Entity: EntityType](entity: E): Boolean = {
    localAddModificationIds(implicitly[EntityType[E]]) contains entity.id
  }

  // **************** Setters ****************//
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    val writeFuture = async {
      require(!isCallingListeners)

      allLocallyCreatedModifications ++= modifications

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) += modification.entityId
      val listeners = invokeListenersAsync(_.modificationsAdded(modifications))

      await(remoteDatabaseProxy.persistEntityModifications(modifications))

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) -= modification.entityId

      await(listeners)
    }
    writeReadyFutures += writeFuture
    writeFuture map { _ =>
      writeReadyFutures -= writeFuture
    }
    writeFuture
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
      val somethingChanged = !response.changes.forall(allLocallyCreatedModifications)

      if (somethingChanged) {
        await(invokeListenersAsync(_.modificationsAdded(response.changes)))
      }
    }
    response.nextUpdateToken
  }
}
