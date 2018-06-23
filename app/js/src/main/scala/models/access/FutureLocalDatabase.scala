package models.access

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import common.ScalaUtils.visibleForTesting
import models.Entity
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala2js.Converters._

/** Wrapper around a LocalDatabase future that allows to attach updates to it. */
private[access] final class FutureLocalDatabase(unsafeLocalDatabaseFuture: Future[LocalDatabase]) {

  private val pendingUpdates: mutable.Buffer[LocalDatabase => Future[Unit]] = mutable.Buffer()
  private var updateInProgress: Boolean = false
  private var lastUpdateDonePromise: Promise[Unit] = Promise.successful((): Unit)

  unsafeLocalDatabaseFuture.map(performPendingUpdates)

  /**
    * Returns future for the LocalDatabase.
    *
    * @param safe If true, errors thrown by the database future are caught and translated into a never-ending
    *     future
    * @param includesLatestUpdates If true, the future completes when there are no more scheduled updates
    */
  def future(safe: Boolean = true, includesLatestUpdates: Boolean = true): Future[LocalDatabase] = async {
    val db = await {
      if (safe) safeLocalDatabaseFuture else unsafeLocalDatabaseFuture
    }

    if (includesLatestUpdates) {
      await(lastUpdateDonePromise.future)
    }

    db
  }

  /**
    * Returns the LocalDatabase if it is ready.
    *
    * @param includesLatestUpdates If true, the LocalDatabase is only ready when there are no more scheduled
    *     updates
    */
  def option(includesLatestUpdates: Boolean = true): Option[LocalDatabase] = {
    future(includesLatestUpdates = includesLatestUpdates).value.map(_.get)
  }

  /**
    * Schedule an operation to be executed immediately after the LocalDatabase has loaded.
    *
    * Note: If the LocalDatabase has already loaded, the given function is executed ASAP.
    */
  def addUpdateAtStart(func: LocalDatabase => Future[Unit]): Unit = {
    pendingUpdates.prepend(func)
    lastUpdateDonePromise = Promise()
    unsafeLocalDatabaseFuture.map(performPendingUpdates)
  }

  /**
    * Schedule an operation to be executed when the LocalDatabase has loaded and all other updates are done.
    *
    * Note: If the LocalDatabase has already loaded and all other updates are done, the given function is
    * executed ASAP.
    */
  def addUpdateAtEnd(func: LocalDatabase => Future[Unit]): Unit = {
    pendingUpdates += func
    lastUpdateDonePromise = Promise()
    unsafeLocalDatabaseFuture.map(performPendingUpdates)
  }

  private def performPendingUpdates(db: LocalDatabase): Unit = {
    if (!updateInProgress) {
      if (pendingUpdates.isEmpty) {
        lastUpdateDonePromise.trySuccess((): Unit)
      } else {
        val update = pendingUpdates.remove(0)
        updateInProgress = true
        async {
          await(update(db))
          updateInProgress = false
          performPendingUpdates(db)
        }
      }
    }
  }

  private val safeLocalDatabaseFuture: Future[LocalDatabase] =
    unsafeLocalDatabaseFuture.recoverWith {
      case t: Throwable =>
        console.log(s"  Could not create local database: $t")
        t.printStackTrace()
        // Fallback to infinitely running future so that API based lookup is always used as fallback
        Promise[LocalDatabase]().future
    }
}
