package hydro.common.testing

import hydro.common.Listenable
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity
import hydro.models.access.DbResultSet
import hydro.models.access.DbQueryExecutor
import hydro.models.access.DbQuery
import hydro.models.access.JsEntityAccess
import hydro.models.access.JsEntityAccess.Listener
import hydro.models.access.PendingModifications

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.scalajs.js

class FakeJsEntityAccess extends JsEntityAccess {

  private val modificationsBuffer: ModificationsBuffer = new ModificationsBuffer()
  private var _pendingModifications: PendingModifications =
    PendingModifications(Seq(), persistedLocally = false)
  private val listeners: mutable.Buffer[Listener] = mutable.Buffer()
  private var queryDelay: FiniteDuration = 0.seconds

  // **************** Implementation of ScalaJsApiClient trait ****************//
  override def newQuery[E <: Entity: EntityType]() = {
    def addDelay[T](future: Future[T]): Future[T] = {
      val resultPromise = Promise[T]()
      js.timers.setTimeout(queryDelay) {
        resultPromise.completeWith(future)
      }
      resultPromise.future
    }

    val delegate = queryExecutor[E].asAsync
    DbResultSet.fromExecutor(new DbQueryExecutor.Async[E] {
      override def data(dbQuery: DbQuery[E]): Future[Seq[E]] = addDelay(delegate.data(dbQuery))
      override def count(dbQuery: DbQuery[E]): Future[Int] = addDelay(delegate.count(dbQuery))
    })
  }
  override def pendingModifications: PendingModifications = _pendingModifications
  override def persistModifications(modifications: Seq[EntityModification]): Future[Unit] = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.modificationsAddedOrPendingStateChanged(modifications))
    Future.successful((): Unit)
  }
  override def clearLocalDatabase(): Future[Unit] = ???
  override def registerListener(listener: Listener): Unit = {
    listeners += listener
  }
  override def deregisterListener(listener: Listener): Unit = {
    listeners -= listener
  }
  override def startCheckingForModifiedEntityUpdates(): Unit = ???

  override def localDatabaseHasBeenLoaded: Listenable[Boolean] = Listenable.fixed(false)

  // **************** Additional methods for tests ****************//
  def newQuerySync[E <: Entity: EntityType](): DbResultSet.Sync[E] = DbResultSet.fromExecutor(queryExecutor)

  // TODO: Add manipulation methods for _pendingModifications
  def addRemoteModifications(modifications: Seq[EntityModification]): Unit = {
    modificationsBuffer.addModifications(modifications)
    listeners.foreach(_.modificationsAddedOrPendingStateChanged(modifications))
  }

  def addRemotelyAddedEntities[E <: Entity: EntityType](entities: E*): Unit = {
    addRemotelyAddedEntities(entities.toVector)
  }

  def addWithRandomId[E <: Entity: EntityType](entityWithoutId: E): E = {
    val entity = Entity.withId(EntityModification.generateRandomId(), entityWithoutId)
    addRemotelyAddedEntities(entity)
    entity
  }

  def addRemotelyAddedEntities[E <: Entity: EntityType](entities: Seq[E]): Unit = {
    addRemoteModifications(entities map (e => EntityModification.Add(e)))
  }

  def allModifications: Seq[EntityModification] = modificationsBuffer.getModifications()

  def queryExecutor[E <: Entity: EntityType]: DbQueryExecutor.Sync[E] =
    DbQueryExecutor.fromEntities(modificationsBuffer.getAllEntitiesOfType[E])

  def slowDownQueries(duration: FiniteDuration): Unit = { queryDelay = duration }
}
