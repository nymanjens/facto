package hydro.models.access

import app.common.testing.TestObjects.testUser
import app.common.testing._
import app.models.access.ModelFields
import hydro.common.testing._
import hydro.models.access.InMemoryEntityDatabase.EntitiesFetcher
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import hydro.models.Entity
import hydro.models.access.DbQueryImplicits._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class InMemoryEntityDatabaseTest extends HookedSpecification {

  private val entitiesFetcher = new FakeEntitiesFetcher

  val user1 = createUser(loginName = "login1", name = "name3")
  val user2 = createUser(loginName = "login2", name = "name2")
  val user3 = createUser(loginName = "login3", name = "name1")

  "queryExecutor()" in {
    entitiesFetcher.users ++= Seq(user1, user2, user3)

    val database = new InMemoryEntityDatabase(entitiesFetcher)

    runTestsAssumingUser123(database)
  }

  "update()" in {
    "Add" in {
      entitiesFetcher.users ++= Seq(user1, user2)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[User])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.users += user3
      database.update(EntityModification.Add(user3))

      runTestsAssumingUser123(database)
    }

    "Remove" in {
      val user4 = createUser(loginName = "login4", name = "name0")

      entitiesFetcher.users ++= Seq(user1, user2, user3, user4)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[User])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.users -= user4
      database.update(EntityModification.createDelete(user4))

      runTestsAssumingUser123(database)
    }

    "Update" in {
      val user2AtCreate = createUser(id = user2.id, loginName = "login9", name = "name99")

      entitiesFetcher.users ++= Seq(user1, user2AtCreate, user3)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[User])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.users -= user2AtCreate
      entitiesFetcher.users += user2
      database.update(EntityModification.Update(user2))

      runTestsAssumingUser123(database)
    }
  }

  private def runTestsAssumingUser123(database: InMemoryEntityDatabase) = {
    val executor = database.queryExecutor[User]

    "filtered" in {
      DbResultSet
        .fromExecutor(executor)
        .filter(ModelFields.User.name === "name2")
        .data() mustEqual Seq(user2)
    }
  }

  private def createUser(id: Long = -1, loginName: String, name: String): User = {
    testUser.copy(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      loginName = loginName,
      name = name
    )
  }

  private class FakeEntitiesFetcher extends EntitiesFetcher {
    val users: mutable.Set[User] = mutable.Set()

    override def fetch[E <: Entity](entityType: EntityType[E]) = entityType match {
      case User.Type => users.toVector.asInstanceOf[Seq[E]]
      case _         => Seq()
    }
  }
}
