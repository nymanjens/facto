package models.access

import java.time.Duration

import api.ScalaJsApi.UpdateToken
import api.ScalaJsApiClient
import common.time.Clock
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.access.webworker.LocalDatabaseWebWorkerApi
import models.user.User
import org.scalajs.dom.console

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[access] final class HybridRemoteDatabaseProxy(localDatabaseFuture: Future[LocalDatabase])(
    implicit apiClient: ScalaJsApiClient,
    clock: Clock)
    extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = {
    localDatabaseOption match {
      case None =>
        new DbQueryExecutor.Async[E] {
          override def data(dbQuery: DbQuery[E]) = apiClient.executeDataQuery(dbQuery)
          override def count(dbQuery: DbQuery[E]) = apiClient.executeCountQuery(dbQuery)
        }
      case Some(localDatabase) => localDatabase.queryExecutor()
    }
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = async {
    val remotePersistFuture = apiClient.persistEntityModifications(modifications)
    val localPersistFuture = {
      localDatabaseOption match {
        case None => Future.successful((): Unit)
        case Some(localDatabase) =>
          async {
            await(localDatabase.applyModifications(modifications))
            await(localDatabase.save())
          }
      }
    }
    await(remotePersistFuture)
    await(localPersistFuture)
  }

  override def getAndApplyRemotelyModifiedEntities(maybeUpdateToken: Option[UpdateToken]) = async {
    val updateToken: UpdateToken = localDatabaseOption match {
      // Subtract one day because this is the maximum time zone difference
      case None => maybeUpdateToken getOrElse clock.now.plus(Duration.ofDays(-1))
      // Don't use given token because after the database is ready, we want to make sure to update
      // since the last update
      case Some(localDatabase) => await(localDatabase.getSingletonValue(NextUpdateTokenKey).map(_.get))
    }

    val response = await(apiClient.getEntityModifications(updateToken))

    if (localDatabaseOption.isDefined) {
      val localDatabase = localDatabaseOption.get
      val somethingChanged = await(localDatabase.applyModifications(response.modifications))
      if (somethingChanged) {
        await(localDatabase.setSingletonValue(NextUpdateTokenKey, response.nextUpdateToken))
        await(localDatabase.save())
      }
    }

    GetRemotelyModifiedEntitiesResponse(
      changes = response.modifications,
      nextUpdateToken = response.nextUpdateToken)
  }

  private def localDatabaseOption: Option[LocalDatabase] = localDatabaseFuture.value.map(_.get)
}

private[access] object HybridRemoteDatabaseProxy {
  private val localDatabaseAndEntityVersion = "1.0"

  private[access] def create()(implicit user: User,
                               apiClient: ScalaJsApiClient,
                               clock: Clock,
                               webWorker: LocalDatabaseWebWorkerApi): RemoteDatabaseProxy = {
    val dbFuture = async {
      val db = await(LocalDatabase.create(encryptionSecret = user.databaseEncryptionKey))
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
        await(db.clear())

        // Set version
        await(db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion))

        // Add all entities
        val allEntitiesResponse = await(apiClient.getAllEntities(EntityType.values))
        await {
          Future.sequence {
            for (entityType <- allEntitiesResponse.entityTypes) yield {
              def addAllToDb[E <: Entity](implicit entityType: EntityType[E]) =
                db.addAll(allEntitiesResponse.entities(entityType))
              addAllToDb(entityType)
            }
          }
        }

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
    new HybridRemoteDatabaseProxy(dbFuture)
  }

}
