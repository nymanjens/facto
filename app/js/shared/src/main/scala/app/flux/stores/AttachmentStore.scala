package app.flux.stores

import app.flux.action.AppActions.AddTransactionGroup
import app.flux.action.AppActions.RefactorAction
import app.flux.action.AppActions.RemoveTransactionGroup
import app.flux.action.AppActions.UpdateTransactionGroup
import app.models.access.AppDbQuerySorting
import app.models.access.AppJsEntityAccess
import app.models.access.ModelFields
import app.models.accounting._
import app.models.accounting.config.Config
import hydro.common.time.Clock
import hydro.flux.action.Dispatcher
import hydro.models.access.DbQueryImplicits._
import hydro.models.modification.EntityModification
import org.scalajs.dom.console

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray.ArrayBuffer

final class AttachmentStore(implicit
    entityAccess: AppJsEntityAccess
) {
  def storeFileAndReturnHash(bytes: ArrayBuffer): Future[String] = {
    Future.never
  }
}
