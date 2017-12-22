package models.user

import models.access.DbQueryImplicits._
import models.access.{Fields, RemoteDatabaseProxy}
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala2js.Converters._

final class JsUserManager(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager {

  override def findByLoginName(loginName: String) = {
    database.newQuery[User]().filter(Fields.User.loginName isEqualTo loginName).data() match {
      case Seq(user) => Option(user)
      case Seq() => None
    }
  }
}
