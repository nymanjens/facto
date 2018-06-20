package models.access

import common.ScalaUtils.visibleForTesting
import models.Entity
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom.console

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.concurrent.{Future, Promise}
import scala2js.Converters._

/** TODO */
final class FutureLocalDatabase(unsafeLocalDatabaseFuture: Future[LocalDatabase]) {

  /** TODO */
  def future(safe: Boolean = true, includesLatestUpdates: Boolean = true): Future[LocalDatabase] = ???

  /** TODO */
  def option(includesLatestUpdates: Boolean = true): Option[LocalDatabase] = ???

  /** TODO */
  def addUpdateAtStart(func: LocalDatabase => Future[Unit]): Unit = ???

  /** TODO */
  def addUpdateAtEnd(func: LocalDatabase => Future[Unit]): Unit = ???

  //  private def safeLocalDatabaseFuture: Future[LocalDatabase] =
//    unsafeLocalDatabaseFuture.recoverWith {
//      case t: Throwable =>
//        console.log(s"  Could not create local database: $t")
//        t.printStackTrace()
//        // Fallback to infinitely running future so that API based lookup is always used as fallback
//        Promise[LocalDatabase]().future
//    }
//
//  private def fullyLoadedLocalDatabaseFuture: Future[LocalDatabase] = async {
//    val db = await(safeLocalDatabaseFuture)
//    await(extraWorkBeforeDatabaseReady)
//    db
//  }
//
//  private def localDatabaseOption: Option[LocalDatabase] = fullyLoadedLocalDatabaseFuture.value.map(_.get)
}
