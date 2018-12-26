package app.api

import java.nio.ByteBuffer

import app.api.Picklers._
import app.api.ScalaJsApi._
import autowire._
import boopickle.Default._
import common.LoggingUtils.logExceptions
import common.websocket.SerialWebsocketClientParallelizer
import app.models.Entity
import app.models.access.DbQuery
import app.models.modification.EntityModification
import app.models.modification.EntityType
import app.models.modification.EntityModification
import app.models.modification.EntityType
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray._

trait ScalaJsApiClient {

  def getInitialData(): Future[GetInitialDataResponse]
  def getAllEntities(types: Seq[EntityType.any]): Future[GetAllEntitiesResponse]
  def persistEntityModifications(modifications: Seq[EntityModification]): Future[Unit]
  def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]): Future[Seq[E]]
  def executeCountQuery(dbQuery: DbQuery[_ <: Entity]): Future[Int]
  def upsertUser(userPrototype: UserPrototype): Future[Unit]
}

object ScalaJsApiClient {

  final class Impl extends ScalaJsApiClient {

    override def getInitialData() = {
      HttpGetAutowireClient[ScalaJsApi].getInitialData().call()
    }

    override def getAllEntities(types: Seq[EntityType.any]) = {
      WebsocketAutowireClient[ScalaJsApi].getAllEntities(types).call()
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]) = {
      HttpPostAutowireClient[ScalaJsApi].persistEntityModifications(modifications).call()
    }

    override def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      WebsocketAutowireClient[ScalaJsApi]
        .executeDataQuery(picklableDbQuery)
        .call()
        .map(_.asInstanceOf[Seq[E]])
    }

    override def executeCountQuery(dbQuery: DbQuery[_ <: Entity]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      WebsocketAutowireClient[ScalaJsApi].executeCountQuery(picklableDbQuery).call()
    }

    override def upsertUser(userPrototype: UserPrototype) = {
      WebsocketAutowireClient[ScalaJsApi].upsertUser(userPrototype).call()
    }

    private object HttpPostAutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      override def doCall(req: Request): Future[ByteBuffer] = {
        dom.ext.Ajax
          .post(
            url = "/scalajsapi/" + req.path.last,
            data = Pickle.intoBytes(req.args),
            responseType = "arraybuffer",
            headers = Map("Content-Type" -> "application/octet-stream")
          )
          .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }

    private object HttpGetAutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      override def doCall(req: Request): Future[ByteBuffer] = {
        require(req.args.isEmpty)
        dom.ext.Ajax
          .get(
            url = "/scalajsapi/" + req.path.last,
            responseType = "arraybuffer",
            headers = Map("Content-Type" -> "application/octet-stream")
          )
          .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }

    private object WebsocketAutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      private val serialWebsocketClient: SerialWebsocketClientParallelizer =
        new SerialWebsocketClientParallelizer(websocketPath = "websocket/scalajsapi/", numWebsockets = 6)

      override def doCall(req: Request): Future[ByteBuffer] = logExceptions {
        serialWebsocketClient.sendAndReceive(Pickle.intoBytes(ScalaJsApiRequest(req.path.last, req.args)))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }
  }
}
