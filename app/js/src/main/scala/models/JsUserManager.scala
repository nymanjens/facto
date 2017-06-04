package models

import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq

final class JsUserManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager {

  override def findByLoginName(loginName: String) = {
    database.newQuery[User]().find("loginName" -> loginName).data() match {
      case Seq(user) => Option(user)
      case Seq() => None
    }
  }
}
