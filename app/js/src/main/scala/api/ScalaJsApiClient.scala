package api

import java.nio.ByteBuffer

import api.Picklers._
import api.ScalaJsApi.GetAllEntitiesResponse
import api.ScalaJsApiClient.AutowireClient
import autowire._
import boopickle.Default._
import models.accounting.config.Config
import models.manager.{Entity, EntityType}
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.typedarray._

final class ScalaJsApiClient {

  def getAccountingConfig(): Future[Config] = AutowireClient[ScalaJsApi].getAccountingConfig().call()

  def getAllEntities(types: Seq[EntityType.any]): Future[GetAllEntitiesResponse] = {
    AutowireClient[ScalaJsApi].getAllEntities(types).call()
  }

  def insertEntityWithId[E <: Entity : EntityType](entity: E): Future[Unit] = {
    require(entity.idOption.isDefined, s"Gotten an entity without ID ($entity)")
    //    AutowireClient[ScalaJsApi].insertEntityWithId(implicitly[EntityType[E]], entity).call()
    ???
  }
}

object ScalaJsApiClient {
  private object AutowireClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
    override def doCall(req: Request): Future[ByteBuffer] = {
      dom.ext.Ajax.post(
        url = "/scalajsapi/" + req.path.last,
        data = Pickle.intoBytes(req.args),
        responseType = "arraybuffer",
        headers = Map("Content-Type" -> "application/octet-stream")
      ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
    }

    override def read[Result: Pickler](p: ByteBuffer) = Unpickle[Result].fromBytes(p)
    override def write[Result: Pickler](r: Result) = Pickle.intoBytes(r)
  }
}
