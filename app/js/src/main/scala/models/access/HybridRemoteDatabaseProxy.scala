package models.access

import java.time.Duration

import api.ScalaJsApi.{GetInitialDataResponse, UpdateToken}
import api.ScalaJsApiClient
import common.ScalaUtils.visibleForTesting
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.access.webworker.LocalDatabaseWebWorkerApi
import models.user.User
import org.scalajs.dom.console

import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** RemoteDatabaseProxy implementation that queries the remote back-end directly until LocalDatabase
  */
private[access] final class HybridRemoteDatabaseProxy(localDatabaseFuture: Future[LocalDatabase])(
    implicit apiClient: ScalaJsApiClient,
    getInitialDataResponse: GetInitialDataResponse)
    extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = {
    localDatabaseOption match {
      case None =>
        new DbQueryExecutor.Async[E] {
          override def data(dbQuery: DbQuery[E]) =
            hybridCall(
              apiClientCall = apiClient.executeDataQuery(dbQuery),
              localDatabaseCall = _.queryExecutor().data(dbQuery))
          override def count(dbQuery: DbQuery[E]) =
            hybridCall(
              apiClientCall = apiClient.executeCountQuery(dbQuery),
              localDatabaseCall = _.queryExecutor().count(dbQuery))

          private def hybridCall[R](apiClientCall: => Future[R],
                                    localDatabaseCall: LocalDatabase => Future[R]): Future[R] = {
            val resultPromise = Promise[R]()

            for (seq <- apiClientCall) {
              resultPromise.trySuccess(seq)
            }

            for {
              localDatabase <- localDatabaseFuture
              if !resultPromise.isCompleted
              seq <- localDatabaseCall(localDatabase)
            } resultPromise.trySuccess(seq)

            resultPromise.future
          }
        }
      case Some(localDatabase) => localDatabase.queryExecutor()
    }
  }

  override def pendingModifications(): Future[Seq[EntityModification]] = async {
    val localDatabase = await(localDatabaseFuture) // "Pending modifications" make no sense without a local database
    await(localDatabase.pendingModifications())
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {
    val serverUpdated = apiClient.persistEntityModifications(modifications)

    localDatabaseOption match {
      case None =>
        // Apply changes to local database, but don't wait for it
        async {
          val localDatabase = await(localDatabaseFuture)
          await(localDatabase.applyModifications(modifications))
          await(localDatabase.addPendingModifications(modifications))
        }
        PersistEntityModificationsResponse(
          queryReflectsModifications = serverUpdated,
          completelyDone = serverUpdated)

      case Some(localDatabase) =>
        val queryReflectsModifications = async {
          await(localDatabase.applyModifications(modifications))
          await(localDatabase.addPendingModifications(modifications))
        }
        val completelyDone = async {
          await(queryReflectsModifications)
          await(localDatabaseOption.get.save())
          await(serverUpdated)
        }
        PersistEntityModificationsResponse(
          queryReflectsModifications = queryReflectsModifications,
          completelyDone = completelyDone)
    }
  }

  override def getAndApplyRemotelyModifiedEntities(maybeUpdateToken: Option[UpdateToken]) = async {
    val updateToken: UpdateToken = localDatabaseOption match {
      case None => maybeUpdateToken getOrElse getInitialDataResponse.nextUpdateToken
      // Don't use given token because after the database is ready, we want to make sure to update
      // since the last local DB update
      case Some(localDatabase) => await(localDatabase.getSingletonValue(NextUpdateTokenKey).map(_.get))
    }

    val response = await(apiClient.getEntityModifications(updateToken))

    if (localDatabaseOption.isDefined && response.modifications.nonEmpty) {
      val localDatabase = localDatabaseOption.get
      await(localDatabase.applyModifications(response.modifications))
      await(localDatabase.removePendingModifications(response.modifications))
      await(localDatabase.setSingletonValue(NextUpdateTokenKey, response.nextUpdateToken))
      await(localDatabase.save())
    }

    GetRemotelyModifiedEntitiesResponse(
      changes = response.modifications,
      nextUpdateToken = response.nextUpdateToken)
  }

  @visibleForTesting private[access] def localDatabaseReadyFuture: Future[Unit] =
    localDatabaseFuture.map(_ => (): Unit)

  private def localDatabaseOption: Option[LocalDatabase] = localDatabaseFuture.value.map(_.get)
}

private[access] object HybridRemoteDatabaseProxy {
  private val localDatabaseAndEntityVersion = "2.0"

  private[access] def create(localDatabase: Future[LocalDatabase])(
      implicit apiClient: ScalaJsApiClient,
      getInitialDataResponse: GetInitialDataResponse): HybridRemoteDatabaseProxy = {
    val dbFuture =
      async {
        val db = await(localDatabase)
        val populateIsNecessary = {
          if (await(db.isEmpty)) {
            console.log(s"  Database is empty")
            true
          } else {
            val dbVersionOption = await(db.getSingletonValue(VersionKey))
            if (!dbVersionOption.contains(localDatabaseAndEntityVersion)) {
              console.log(
                s"  The database version ${dbVersionOption getOrElse "<empty>"} no longer matches " +
                  s"the newest version $localDatabaseAndEntityVersion")
              true
            } else {
              console.log(s"  Database was loaded successfully. No need for a full repopulation.")
              false
            }
          }
        }
        if (populateIsNecessary) {
          console.log(s"  Populating database...")

          // Reset database
          await(db.resetAndInitialize())

          // Set version
          await(db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion))

          // Add all entities
          val allEntitiesResponse = await(apiClient.getAllEntities(EntityType.values))
          val _ = await(Future.sequence {
            for (entityType <- allEntitiesResponse.entityTypes) yield {
              def addAllToDb[E <: Entity](implicit entityType: EntityType[E]) =
                db.addAll(allEntitiesResponse.entities(entityType))
              addAllToDb(entityType)
            }
          })

          await(db.setSingletonValue(NextUpdateTokenKey, allEntitiesResponse.nextUpdateToken))

          // Await because we don't want to save unpersisted modifications that can be made as soon as
          // the database becomes valid.
          await(db.save())
          console.log(s"  Population done!")
          db
        } else {
          db
        }
      }
    val safeDbFuture = dbFuture.recoverWith {
      case t: Throwable =>
        console.log(s"  Could not create local database: $t")
        t.printStackTrace()
        // Fallback to infinitely running future so that API based lookup is always used as fallback
        Promise[LocalDatabase]().future
    }
    new HybridRemoteDatabaseProxy(safeDbFuture)
  }

}
