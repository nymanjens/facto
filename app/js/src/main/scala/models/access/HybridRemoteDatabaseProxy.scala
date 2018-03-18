package models.access

import api.ScalaJsApi.UpdateToken
import api.ScalaJsApiClient
import common.time.Clock
import models.Entity
import models.modification.{EntityModification, EntityType}

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.user.User
import org.scalajs.dom.console

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

private[access] final class HybridRemoteDatabaseProxy(localDatabaseFuture: Future[LocalDatabase])(
    implicit apiClient: ScalaJsApiClient)
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
    val localDatabase = await(localDatabaseFuture)
    val updateToken: UpdateToken = await {
      maybeUpdateToken match {
        case None        => localDatabase.getSingletonValue(NextUpdateTokenKey).map(_.get)
        case Some(token) => Future.successful(token)
      }
    }
    val response = await(apiClient.getEntityModifications(updateToken))

    val somethingChanged = await(localDatabase.applyModifications(response.modifications))
    if (somethingChanged) {
      await(localDatabase.setSingletonValue(NextUpdateTokenKey, response.nextUpdateToken))
      await(localDatabase.save())
    }

    GetRemotelyModifiedEntitiesResponse(
      changes = response.modifications,
      nextUpdateToken = response.nextUpdateToken)
  }

  private def localDatabaseOption: Option[LocalDatabase] = localDatabaseFuture.value.map(_.get)
}

//private[access] object LocallyClonedJsEntityAccess {
//  private val localDatabaseAndEntityVersion = "1.0"
//
//  private[access] def create(apiClient: ScalaJsApiClient,
//                             possiblyEmptyLocalDatabase: LocalDatabase,
//                             allUsers: Seq[User]): Future[LocallyClonedJsEntityAccess] =
//    async {
//      val db = possiblyEmptyLocalDatabase
//      val populatedDb = {
//        val populateIsNecessary = {
//          if (db.isEmpty) {
//            console.log(s"  Database is empty")
//            true
//          } else if (!db.getSingletonValue(VersionKey).contains(localDatabaseAndEntityVersion)) {
//            console.log(
//              s"  The database version ${db.getSingletonValue(VersionKey) getOrElse "<empty>"} no longer matches " +
//                s"the newest version $localDatabaseAndEntityVersion")
//            true
//          } else {
//            console.log(s"  Database was loaded successfully. No need for a full repopulation.")
//            false
//          }
//        }
//        if (populateIsNecessary) {
//          console.log(s"  Populating database...")
//
//          // Reset database
//          await(db.clear())
//
//          // Set version
//          db.setSingletonValue(VersionKey, localDatabaseAndEntityVersion)
//
//          // Add all entities
//          val allEntitiesResponse = await(apiClient.getAllEntities(EntityType.values))
//          for (entityType <- allEntitiesResponse.entityTypes) {
//            def addAllToDb[E <: Entity](implicit entityType: EntityType[E]) =
//              db.addAll(allEntitiesResponse.entities(entityType))
//            addAllToDb(entityType)
//          }
//          db.setSingletonValue(NextUpdateTokenKey, allEntitiesResponse.nextUpdateToken)
//
//          // Await because we don't want to save unpersisted modifications that can be made as soon as
//          // the database becomes valid.
//          await(db.save())
//          console.log(s"  Population done!")
//          db
//        } else {
//          db
//        }
//      }
//      new LocallyClonedJsEntityAccess(apiClient, populatedDb, allUsers)
//    }
//
//}
