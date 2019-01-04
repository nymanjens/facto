package app.controllers.helpers

import java.nio.ByteBuffer

import app.api.ScalaJsApi.UserPrototype
import app.api.ScalaJsApiServerFactory
import hydro.controllers.InternalApi.ScalaJsApiCaller
import app.models.user.User
import boopickle.Default._
import app.api.Picklers._
import com.google.inject.Inject
import hydro.api.PicklableDbQuery
import hydro.models.Entity
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType

import scala.collection.immutable.Seq

final class ScalaJsApiCallerImpl @Inject()(implicit scalaJsApiServerFactory: ScalaJsApiServerFactory)
    extends ScalaJsApiCaller {

  override def apply(path: String, argsMap: Map[String, ByteBuffer])(implicit user: User): ByteBuffer = {
    val scalaJsApiServer = scalaJsApiServerFactory.create()

    path match {
      case "getInitialData" =>
        Pickle.intoBytes(scalaJsApiServer.getInitialData())
      case "getAllEntities" =>
        val types = Unpickle[Seq[EntityType.any]].fromBytes(argsMap("types"))
        Pickle.intoBytes(scalaJsApiServer.getAllEntities(types))
      case "persistEntityModifications" =>
        val modifications = Unpickle[Seq[EntityModification]].fromBytes(argsMap("modifications"))
        Pickle.intoBytes(scalaJsApiServer.persistEntityModifications(modifications))
      case "executeDataQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes[Seq[Entity]](scalaJsApiServer.executeDataQuery(dbQuery))
      case "executeCountQuery" =>
        val dbQuery = Unpickle[PicklableDbQuery].fromBytes(argsMap("dbQuery"))
        Pickle.intoBytes(scalaJsApiServer.executeCountQuery(dbQuery))
      case "upsertUser" =>
        val userPrototype = Unpickle[UserPrototype].fromBytes(argsMap("userPrototype"))
        Pickle.intoBytes(scalaJsApiServer.upsertUser(userPrototype))
    }
  }
}
