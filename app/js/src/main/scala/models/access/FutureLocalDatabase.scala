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

/** TODO */
final class FutureLocalDatabase(unsafeLocalDatabaseFuture: Future[LocalDatabase]) {

  private val pendingUpdates: mutable.Buffer[LocalDatabase => Future[Unit]] = mutable.Buffer()
  private var updateInProgress: Boolean = false
  private var lastUpdateDonePromise: Promise[Unit] = Promise.successful((): Unit)

  unsafeLocalDatabaseFuture.map(performPendingUpdates)

  /** TODO */
  def future(safe: Boolean = true, includesLatestUpdates: Boolean = true): Future[LocalDatabase] = async {
    val db = await {
      if (safe) safeLocalDatabaseFuture else unsafeLocalDatabaseFuture
    }

    if (includesLatestUpdates) {
      await(lastUpdateDonePromise.future)
    }

    db
  }

  /** TODO */
  def option(includesLatestUpdates: Boolean = true): Option[LocalDatabase] = {
    future(includesLatestUpdates = includesLatestUpdates).value.map(_.get)
  }

  /** TODO */
  // warning if already finished finished - if so, execute func immediately
  def addUpdateAtStart(func: LocalDatabase => Future[Unit]): Unit = {
    pendingUpdates.prepend(func)
    lastUpdateDonePromise = Promise()
    unsafeLocalDatabaseFuture.map(performPendingUpdates)
  }

  /** TODO */
  // warning if already finished finished - if so, execute func immediately
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
