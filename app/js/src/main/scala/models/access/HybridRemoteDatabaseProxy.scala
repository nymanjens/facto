package models.access

import api.ScalaJsApi.GetInitialDataResponse
import api.ScalaJsApiClient
import common.LoggingUtils.logFailure
import models.Entity
import models.access.SingletonKey.{NextUpdateTokenKey, VersionKey}
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** RemoteDatabaseProxy implementation that queries the remote back-end directly until LocalDatabase
  */
private[access] final class HybridRemoteDatabaseProxy(futureLocalDatabase: FutureLocalDatabase)(
    implicit apiClient: ScalaJsApiClient,
    getInitialDataResponse: GetInitialDataResponse)
    extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = {
    futureLocalDatabase.option() match {
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

            for (seq <- logFailure(apiClientCall)) {
              resultPromise.trySuccess(seq)
            }

            for {
              localDatabase <- futureLocalDatabase.future()
              if !resultPromise.isCompleted
              seq <- logFailure(localDatabaseCall(localDatabase))
            } resultPromise.trySuccess(seq)

            resultPromise.future
          }
        }
      case Some(localDatabase) => localDatabase.queryExecutor()
    }
  }

  override def pendingModifications(): Future[Seq[EntityModification]] = async {
    val localDatabase = await(futureLocalDatabase.future()) // "Pending modifications" make no sense without a local database
    await(localDatabase.pendingModifications())
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {
    val serverUpdated = apiClient.persistEntityModifications(modifications)

    futureLocalDatabase.option() match {
      case None =>
        // Apply changes to local database, but don't wait for it
        futureLocalDatabase.scheduleUpdateAtEnd(localDatabase =>
          async {
            await(localDatabase.applyModifications(modifications))
            await(localDatabase.addPendingModifications(modifications))
        })
        PersistEntityModificationsResponse(
          queryReflectsModificationsFuture = serverUpdated,
          completelyDoneFuture = serverUpdated)

      case Some(localDatabase) =>
        val queryReflectsModifications = async {
          await(localDatabase.applyModifications(modifications))
          await(localDatabase.addPendingModifications(modifications))
        }
        val completelyDone = async {
          await(queryReflectsModifications)
          await(localDatabase.save())
          await(serverUpdated)
        }
        PersistEntityModificationsResponse(
          queryReflectsModificationsFuture = queryReflectsModifications,
          completelyDoneFuture = completelyDone)
    }
  }

  override def startCheckingForModifiedEntityUpdates(
      maybeNewEntityModificationsListener: Seq[EntityModification] => Future[Unit]): Unit = {
    val temporaryPushClient = new EntityModificationPushClient(
      name = "EntityModificationPush[temporary]",
      updateToken = getInitialDataResponse.nextUpdateToken,
      onMessageReceived = modificationsWithToken =>
        async {
          val modifications = modificationsWithToken.modifications
          console.log(s"  [temporary push client] ${modifications.size} remote modifications received")
          await(maybeNewEntityModificationsListener(modifications))
      }
    )

    // Adding at start here because old modifications were already reflected in API lookups
    futureLocalDatabase.scheduleUpdateAtStart(localDatabase =>
      async {
        val storedUpdateToken = await(localDatabase.getSingletonValue(NextUpdateTokenKey).map(_.get))
        temporaryPushClient.close()

        val permanentPushClient = new EntityModificationPushClient(
          name = "EntityModificationPush[permanent]",
          updateToken = storedUpdateToken,
          onMessageReceived = modificationsWithToken =>
            async {
              val modifications = modificationsWithToken.modifications
              console.log(s"  [permanent push client] ${modifications.size} remote modifications received")
              if (modifications.nonEmpty) {
                await(localDatabase.applyModifications(modifications))
                await(localDatabase.removePendingModifications(modifications))
                await(
                  localDatabase.setSingletonValue(NextUpdateTokenKey, modificationsWithToken.nextUpdateToken))
                await(localDatabase.save())
              }
              await(maybeNewEntityModificationsListener(modifications))
          }
        )
        await(permanentPushClient.firstMessageWasProcessedFuture)
    })
  }

  override def clearLocalDatabase(): Future[Unit] = {
    val clearFuture = async {
      val localDatabase = await(futureLocalDatabase.future(safe = false, includesLatestUpdates = false))
      await(localDatabase.resetAndInitialize())
      await(localDatabase.save())
    }
    clearFuture.recover {
      case t: Throwable =>
        console.log(s"  Could not clear local database: $t")
        t.printStackTrace()
        // Fall back to successful future
        (): Unit
    }
  }

  override def localDatabaseReadyFuture: Future[Unit] = futureLocalDatabase.future().map(_ => (): Unit)
}

private[access] object HybridRemoteDatabaseProxy {
  private val localDatabaseAndEntityVersion = "3.0"

  private[access] def create(localDatabase: Future[LocalDatabase])(
      implicit apiClient: ScalaJsApiClient,
      getInitialDataResponse: GetInitialDataResponse): HybridRemoteDatabaseProxy = {
    new HybridRemoteDatabaseProxy(new FutureLocalDatabase(async {
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
      }
      db
    }))
  }

}
