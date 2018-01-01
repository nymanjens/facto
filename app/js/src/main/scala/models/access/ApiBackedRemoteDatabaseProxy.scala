package models.access

import api.ScalaJsApi.UpdateToken
import api.ScalaJsApiClient
import common.LoggingUtils.logExceptions
import common.ScalaUtils.visibleForTesting
import common.time.Clock
import models.Entity
import models.access.RemoteDatabaseProxy.Listener
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

private[access] final class ApiBackedRemoteDatabaseProxy(implicit apiClient: ScalaJsApiClient, clock: Clock)
    extends RemoteDatabaseProxy {

  private var listeners: Seq[Listener] = Seq()
  private val localAddModificationIds: Map[EntityType.any, mutable.Set[Long]] =
    EntityType.values.map(t => t -> mutable.Set[Long]()).toMap
  private val allLocallyCreatedModifications: mutable.Set[EntityModification] = mutable.Set()
  private var isCallingListeners: Boolean = false
  private var lastWriteFuture: Future[Unit] = Future.successful((): Unit)

  // **************** Getters ****************//
  override def newQuery[E <: Entity: EntityType](): DbResultSet.Async[E] = {
    DbResultSet.fromExecutor(new DbQueryExecutor.Async[E] {
      override def dataAsync(dbQuery: DbQuery[E]) = async {
        await(lastWriteFuture)
        await(apiClient.executeDataQuery(dbQuery))
      }
      override def countAsync(dbQuery: DbQuery[E]) = async {
        await(lastWriteFuture)
        await(apiClient.executeCountQuery(dbQuery))
      }
    })
  }

  override def hasLocalAddModifications[E <: Entity: EntityType](entity: E): Boolean = {
    localAddModificationIds(implicitly[EntityType[E]]) contains entity.id
  }

  // **************** Setters ****************//
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    lastWriteFuture = async {
      require(!isCallingListeners)

      allLocallyCreatedModifications ++= modifications

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) += modification.entityId
      val listeners1 = invokeListenersAsync(_.addedLocally(modifications))

      val apiFuture = apiClient.persistEntityModifications(modifications)

      await(apiFuture)

      for {
        modification <- modifications
        if modification.isInstanceOf[EntityModification.Add[_]]
      } localAddModificationIds(modification.entityType) -= modification.entityId
      val listeners2 = invokeListenersAsync(_.localModificationPersistedRemotely(modifications))

      await(listeners1)
      await(listeners2)
    }
    lastWriteFuture
  }

  override def clearLocalDatabase(): Future[Unit] = Future.successful((): Unit)

  // **************** Other ****************//
  override def registerListener(listener: Listener): Unit = {
    require(!isCallingListeners)

    listeners = listeners :+ listener
  }

  override private[access] def startSchedulingModifiedEntityUpdates(): Unit = {
    var timeout = 5.seconds
    def cyclicLogic(updateToken: UpdateToken): Unit = {
      updateModifiedEntities(updateToken) map { nextUpdateToken =>
        js.timers.setTimeout(timeout)(cyclicLogic(nextUpdateToken))
        timeout * 1.02
      }
    }

    js.timers.setTimeout(0)(cyclicLogic(updateToken = clock.now))
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
      updateToken: UpdateToken): Future[UpdateToken] = async {
    val response = await(apiClient.getEntityModifications(updateToken))
    if (response.modifications.nonEmpty) {
      println(s"  ${response.modifications.size} remote modifications received")
      val somethingChanged = !response.modifications.forall(allLocallyCreatedModifications)

      if (somethingChanged) {
        await(invokeListenersAsync(_.addedRemotely(response.modifications)))
      }
    }
    response.nextUpdateToken
  }
}
