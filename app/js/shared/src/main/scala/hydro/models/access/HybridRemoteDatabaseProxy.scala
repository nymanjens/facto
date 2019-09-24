package hydro.models.access

import app.api.ScalaJsApi.GetInitialDataResponse
import app.api.ScalaJsApiClient
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.modification.EntityTypes
import hydro.common.JsLoggingUtils.logFailure
import hydro.models.Entity
import hydro.models.access.SingletonKey.NextUpdateTokenKey
import hydro.models.access.SingletonKey.VersionKey
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/** RemoteDatabaseProxy implementation that queries the remote back-end directly until LocalDatabase */
final class HybridRemoteDatabaseProxy(futureLocalDatabase: FutureLocalDatabase)(
    implicit apiClient: ScalaJsApiClient,
    getInitialDataResponse: GetInitialDataResponse,
    hydroPushSocketClientFactory: HydroPushSocketClientFactory,
    entitySyncLogic: EntitySyncLogic,
) extends RemoteDatabaseProxy {

  override def queryExecutor[E <: Entity: EntityType]() = {
    new DbQueryExecutor.Async[E] {
      override def data(dbQuery: DbQuery[E]) =
        execute(dbQuery, apiClientCall = apiClient.executeDataQuery, localDatabaseCall = _ data _)

      override def count(dbQuery: DbQuery[E]) =
        execute(dbQuery, apiClientCall = apiClient.executeCountQuery, localDatabaseCall = _ count _)

      private def execute[R](
          dbQuery: DbQuery[E],
          apiClientCall: DbQuery[E] => Future[R],
          localDatabaseCall: (DbQueryExecutor.Async[E], DbQuery[E]) => Future[R],
      ): Future[R] = {
        futureLocalDatabase.option() match {
          case None =>
            hybridCall(dbQuery, apiClientCall, localDatabaseCall)

          case Some(localDatabase) =>
            async {
              if (await(entitySyncLogic.canBeExecutedLocally(dbQuery, localDatabase))) {
                await(localDatabaseCall(localDatabase.queryExecutor(), dbQuery))
              } else {
                await(apiClientCall(dbQuery))
              }
            }
        }
      }

      private def hybridCall[R](
          dbQuery: DbQuery[E],
          apiClientCall: DbQuery[E] => Future[R],
          localDatabaseCall: (DbQueryExecutor.Async[E], DbQuery[E]) => Future[R],
      ): Future[R] = {
        val resultPromise = Promise[R]()

        for (seq <- logFailure(apiClientCall(dbQuery))) {
          resultPromise.trySuccess(seq)
        }

        for {
          localDatabase <- futureLocalDatabase.future()
          canBeExecutedLocally <- entitySyncLogic.canBeExecutedLocally(dbQuery, localDatabase)
          if canBeExecutedLocally && !resultPromise.isCompleted
          seq <- logFailure(localDatabaseCall(localDatabase.queryExecutor(), dbQuery))
        } resultPromise.trySuccess(seq)

        resultPromise.future
      }
    }
  }

  override def pendingModifications(): Future[Seq[EntityModification]] = async {
    val localDatabase = await(futureLocalDatabase.future()) // "Pending modifications" make no sense without a local database
    await(localDatabase.pendingModifications())
  }

  override def persistEntityModifications(modifications: Seq[EntityModification]) = {

    futureLocalDatabase.option() match {
      case None =>
        val serverUpdated = apiClient.persistEntityModifications(modifications)

        // Apply changes to local database, but don't wait for it
        futureLocalDatabase.scheduleUpdateAtEnd(localDatabase =>
          async {
            await(entitySyncLogic.handleEntityModificationUpdate(modifications, localDatabase))
            await(localDatabase.addPendingModifications(modifications))
            await(localDatabase.save())
        })
        PersistEntityModificationsResponse(
          queryReflectsModificationsFuture = serverUpdated,
          completelyDoneFuture = serverUpdated)

      case Some(localDatabase) =>
        val serverUpdated = async {
          // Also send all pending modifications. If we don't do this, an unsuccessful call for modification A
          // followed by a successful call for modification B will result in B being applied to the server before A.
          val pendingModifications = await(localDatabase.pendingModifications())
          await(apiClient.persistEntityModifications(pendingModifications ++ modifications))
        }

        val queryReflectsModifications = async {
          await(entitySyncLogic.handleEntityModificationUpdate(modifications, localDatabase))
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
    val temporaryPushClient = hydroPushSocketClientFactory.createClient(
      name = "HydroPushSocket[temporary]",
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

        val permanentPushClient = hydroPushSocketClientFactory.createClient(
          name = "HydroPushSocket[permanent]",
          updateToken = storedUpdateToken,
          onMessageReceived = modificationsWithToken =>
            async {
              val modifications = modificationsWithToken.modifications
              console.log(s"  [permanent push client] ${modifications.size} remote modifications received")
              if (modifications.nonEmpty) {
                await(entitySyncLogic.handleEntityModificationUpdate(modifications, localDatabase))
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

object HybridRemoteDatabaseProxy {
  private val localDatabaseAndEntityVersion = "hydro-2.3"

  def create(localDatabase: Future[LocalDatabase])(
      implicit apiClient: ScalaJsApiClient,
      getInitialDataResponse: GetInitialDataResponse,
      hydroPushSocketClientFactory: HydroPushSocketClientFactory,
      entitySyncLogic: EntitySyncLogic,
  ): HybridRemoteDatabaseProxy = {
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

        // Populate with entities
        val nextUpdateToken = await(entitySyncLogic.populateLocalDatabaseAndGetUpdateToken(db))
        await(db.setSingletonValue(NextUpdateTokenKey, nextUpdateToken))

        // Await because we don't want to save unpersisted modifications that can be made as soon as
        // the database becomes valid.
        await(db.save())
        console.log(s"  Population done!")
      }
      db
    }))
  }

}
