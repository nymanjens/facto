package models

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Keys

final class JsUserManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager {

  override def findByLoginName(loginName: String) = {
    database.newQuery[User]().filter(Keys.User.loginName, loginName).data() match {
      case Seq(user) => Option(user)
      case Seq() => None
    }
  }
}
