package api

import autowire._
import models.accounting.config.Config
import java.nio.ByteBuffer

import boopickle.Default._
import api.Picklers._
import api.ScalaJsApi.EntityType
import models.manager.Entity
import org.scalajs.dom

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.typedarray._

final class ScalaJsApiClient {

  import ScalaJsApiClient.AutowireClient

  def getAccountingConfig(): Future[Config] = AutowireClient[ScalaJsApi].getAccountingConfig().call()

  def getAllEntities(types: Seq[EntityType]): Future[Map[EntityType, Seq[Entity[_]]]] =
    AutowireClient[ScalaJsApi].getAllEntities(types).call()

  def insertEntityWithId[E <: Entity[E]](entityType: EntityType[E], entity: E): Future[_] = {
    require(entity.idOption.isDefined, s"Gotten an entity without ID ($entityType, $entity)")
    val entityBytes = Pickle.intoBytes(entity)
    AutowireClient[ScalaJsApi].insertEntityWithId(entityType, entityBytes).call()
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
