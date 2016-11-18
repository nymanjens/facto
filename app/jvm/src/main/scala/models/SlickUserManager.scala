package models

import com.google.common.base.Charsets
import com.google.common.hash.Hashing

import models.SlickUtils.dbApi._
import models.SlickUtils.dbRun
import models.manager.{EntityTable, Entity, SlickEntityManager, ForwardingEntityManager}

import SlickUserManager.{Users, tableName}

final class SlickUserManager extends ForwardingEntityManager[User, Users](
  SlickEntityManager.create[User, Users](
    tag => new Users(tag),
    tableName = tableName,
    cached = true
  )) with User.Manager {

  override def findByLoginName(loginName: String): Option[User] = {
    val users: Seq[User] = dbRun(newQuery.filter(u => u.loginName === loginName).result)
    users match {
      case Seq() => None
      case Seq(u) => Option(u)
    }
  }
}

object SlickUserManager {
  private val tableName: String = "USERS"

  final class Users(tag: Tag) extends EntityTable[User](tag, tableName) {
    def loginName = column[String]("loginName")

    def passwordHash = column[String]("passwordHash")

    def name = column[String]("name")

    override def * = (loginName, passwordHash, name, id.?) <> (User.tupled, User.unapply)
  }
}
