//package models
//
//import com.google.common.base.Charsets
//import com.google.common.hash.Hashing
//
//import models.SlickUtils.dbApi._
//import models.SlickUtils.dbRun
//import models.manager.{EntityTable, Entity, EntityManager, ForwardingEntityManager}
//
//class UserManager extends EntityManager[User, Users](
//  EntityManager.create[User, Users](
//    tag => new Users(tag),
//    tableName = "USERS",
//    cached = true
//  )) {
//
//  private[models] def hash(password: String) = Hashing.sha512().hashString(password, Charsets.UTF_8).toString()
//
//  def newWithUnhashedPw(loginName: String, password: String, name: String): User =
//    new User(loginName, hash(password), name)
//
//  def authenticate(loginName: String, password: String): Boolean = {
//    findByLoginName(loginName) match {
//      case Some(user) if user.passwordHash == hash(password) => true
//      case _ => false
//    }
//  }
//
//  def findByLoginName(loginName: String): Option[User] = {
//    val users: Seq[User] = dbRun(Users.newQuery.filter(u => u.loginName === loginName).result)
//    users match {
//      case Seq() => None
//      case Seq(u) => Option(u)
//    }
//  }
//}
