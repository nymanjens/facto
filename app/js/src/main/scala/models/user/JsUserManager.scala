package models.user

import jsfacades.LokiJsImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class JsUserManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager {

  override def findByIdSync(id: Long) = {
    database.newQuery[User]().findOne(Keys.id, id).value.get.get.get
  }
  override def fetchAllSync() = {
    database.newQuery[User]().data().value.get.get
  }
  override def findByLoginName(loginName: String) = {
    database.newQuery[User]().filter(Keys.User.loginName isEqualTo loginName).data().value.get.get match {
      case Seq(user) => Option(user)
      case Seq() => None
    }
  }
}
