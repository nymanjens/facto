package api

import java.nio.ByteBuffer

import api.Picklers._
import api.ScalaJsApi.{
  GetAllEntitiesResponse,
  GetEntityModificationsResponse,
  GetInitialDataResponse,
  UpdateToken
}
import autowire._
import boopickle.Default._
import models.User
import models.accounting.config.Config
import models.manager.{Entity, EntityModification, EntityType}
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future
import scala.scalajs.js.typedarray._

trait ScalaJsApiClient {

  def getInitialData(): Future[GetInitialDataResponse]
  def getAllEntities(types: Seq[EntityType.any]): Future[GetAllEntitiesResponse]
  def getEntityModifications(updateToken: UpdateToken): Future[GetEntityModificationsResponse]
  def persistEntityModifications(modifications: Seq[EntityModification]): Future[Unit]
}

object ScalaJsApiClient {

  final class Impl extends ScalaJsApiClient {
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
  }

  private object AutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
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
}
