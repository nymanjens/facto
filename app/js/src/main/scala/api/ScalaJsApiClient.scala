package api

import java.nio.ByteBuffer

import api.ScalaJsApi.{
  GetAllEntitiesResponse,
  GetEntityModificationsResponse,
  GetInitialDataResponse,
  UpdateToken
}
import autowire._
import boopickle.Default._
import models.Entity
import models.access.DbQuery
import models.modification.{EntityModification, EntityType}
import org.scalajs.dom
import api.Picklers._
import common.LoggingUtils.logExceptions

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.typedarray._

trait ScalaJsApiClient {

  def getInitialData(): Future[GetInitialDataResponse]
  def getAllEntities(types: Seq[EntityType.any]): Future[GetAllEntitiesResponse]
  def getEntityModifications(updateToken: UpdateToken): Future[GetEntityModificationsResponse]
  def persistEntityModifications(modifications: Seq[EntityModification]): Future[Unit]
  def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]): Future[Seq[E]]
  def executeCountQuery(dbQuery: DbQuery[_ <: Entity]): Future[Int]
}

object ScalaJsApiClient {

  final class Impl extends ScalaJsApiClient {
    private val serialWebsocketClient: SerialWebsocketClientParallelizer =
      new SerialWebsocketClientParallelizer(websocketPath = "scalajsapiwebsocket/", numWebsockets = 5)

    override def getInitialData() = {
      AutowireClient[ScalaJsApi].getInitialData().call()
    }

    override def getAllEntities(types: Seq[EntityType.any]) = {
      AutowireClient[ScalaJsApi].getAllEntities(types).call()
    }

    override def getEntityModifications(updateToken: UpdateToken) = {
      AutowireClient[ScalaJsApi].getEntityModifications(updateToken).call()
    }

    override def persistEntityModifications(modifications: Seq[EntityModification]) = {
      AutowireClient[ScalaJsApi].persistEntityModifications(modifications).call()
    }

    override def executeDataQuery[E <: Entity](dbQuery: DbQuery[E]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      AutowireClient[ScalaJsApi].executeDataQuery(picklableDbQuery).call().map(_.asInstanceOf[Seq[E]])
    }

    override def executeCountQuery(dbQuery: DbQuery[_ <: Entity]) = {
      val picklableDbQuery = PicklableDbQuery.fromRegular(dbQuery)
      AutowireClient[ScalaJsApi].executeCountQuery(picklableDbQuery).call()
    }

    private object AutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
      override def doCall(req: Request): Future[ByteBuffer] = logExceptions {
        serialWebsocketClient.sendAndReceive(Pickle.intoBytes(ScalaJsApiRequest(req.path.last, req.args)))
      }

      override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
      override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
    }
  }
}
