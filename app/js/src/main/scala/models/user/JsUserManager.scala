package models.user

import models.access.DbQueryImplicits._
import models.access.RemoteDatabaseProxy
import models.manager.BaseJsEntityManager

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala2js.Converters._
import models.access.Fields

final class JsUserManager(allUsers: Seq[User])(implicit database: RemoteDatabaseProxy)
    extends BaseJsEntityManager[User]
    with User.Manager {
  private val idToUser: Map[Long, User] = allUsers.map(u => u.id -> u).toMap

  override def findByIdSync(id: Long) = idToUser(id)
  override def fetchAllSync() = allUsers

  override def findByLoginName(loginName: String) = allUsers.find(_.loginName == loginName)
}
