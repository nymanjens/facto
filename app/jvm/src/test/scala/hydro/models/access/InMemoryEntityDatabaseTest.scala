package hydro.models.access

import app.common.testing.TestObjects.testUser
import app.common.testing._
import app.models.access.ModelFields
import hydro.common.testing._
import hydro.models.access.InMemoryEntityDatabase.EntitiesFetcher
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import app.models.user.User
import com.google.inject.Guice
import com.google.inject.Inject
import hydro.models.Entity
import hydro.models.access.DbQuery.Sorting
import hydro.models.access.DbQueryImplicits._
import hydro.models.access.InMemoryEntityDatabase.Sortings
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class InMemoryEntityDatabaseTest extends HookedSpecification {

  private val entitiesFetcher = new FakeEntitiesFetcher

  implicit private val fakeClock: FakeClock = new FakeClock

  private val user1 = createUser(loginName = "login1", name = "name3")
  private val user2 = createUser(loginName = "login2", name = "name2")
  private val user3 = createUser(loginName = "login3", name = "name1")
  private val user4 = createUser(loginName = "login4", name = "name0")

  "queryExecutor()" in {
    entitiesFetcher.setUsers(user1, user2, user3)

    implicit val database = new InMemoryEntityDatabase(
      entitiesFetcher,
      sortings = Sortings.create.withSorting(Sorting.ascBy(ModelFields.User.loginName)))

    "all data" in {
      newUserQuery().data() must containTheSameElementsAs(Seq(user1, user2, user3))
    }
    "filter" in {
      newUserQuery().filter(ModelFields.User.name === "name2").data() mustEqual Seq(user2)
    }
    "limit" in {
      newUserQuery().sort(Sorting.ascBy(ModelFields.User.loginName)).limit(2).data() mustEqual
        Seq(user1, user2)
    }
    "sorting" in {
      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }
  }

  "update()" in {
    "Add" in {
      entitiesFetcher.setUsers(user1, user2)
      implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
      triggerLazyFetching(database)

      database.update(EntityModification.Add(user3))

      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }

    "Remove" in {
      entitiesFetcher.setUsers(user1, user2, user3, user4)
      implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
      triggerLazyFetching(database)

      database.update(EntityModification.createRemove(user4))

      assertDatabaseContainsExactlySorted(user1, user2, user3)
    }

    "Update" in {
      "Full update" in {
        entitiesFetcher.setUsers(user1, user2, user3)
        implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
        triggerLazyFetching(database)

        val user2Update = EntityModification.createUpdateAllFields(user2.copy(loginName = "login2_update"))
        database.update(user2Update)

        assertDatabaseContainsExactlySorted(user1, user2Update.updatedEntity, user3)
      }
      "Partial update" in {
        entitiesFetcher.setUsers(user1, user2, user3)
        implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
        triggerLazyFetching(database)

        val user2UpdateA = EntityModification.createUpdate(
          user2.copy(loginName = "login2_update"),
          fieldMask = Seq(ModelFields.User.loginName))
        val user2UpdateB = EntityModification.createUpdate(
          user2.copy(name = "name2_update"),
          fieldMask = Seq(ModelFields.User.name))
        database.update(user2UpdateA)
        database.update(user2UpdateB)

        assertDatabaseContainsExactlySorted(
          user1,
          user2.copy(
            loginName = "login2_update",
            name = "name2_update",
            lastUpdateTime = user2UpdateA.updatedEntity.lastUpdateTime
              .merge(user2UpdateB.updatedEntity.lastUpdateTime, forceIncrement = false)
          ),
          user3
        )
      }
      "Ignored when already deleted" in {
        entitiesFetcher.setUsers(user1, user3)
        implicit val database = new InMemoryEntityDatabase(entitiesFetcher)
        triggerLazyFetching(database)

        val user2Update = EntityModification.createUpdateAllFields(user2)
        database.update(user2Update)

        assertDatabaseContainsExactlySorted(user1, user3)
      }
    }
  }

  private def assertDatabaseContainsExactlySorted(users: User*)(implicit database: InMemoryEntityDatabase) = {
    "assertDatabaseContainsExactlySorted" in {
      "cached asc sorting" in {
        newUserQuery().sort(Sorting.ascBy(ModelFields.User.loginName)).data() mustEqual users
      }
      "cached desc sorting" in {
        newUserQuery().sort(Sorting.descBy(ModelFields.User.loginName)).data() mustEqual users.reverse
      }
      "non-cached sorting" in {
        newUserQuery().sort(Sorting.ascBy(ModelFields.User.name)).data() mustEqual users.reverse
      }
    }
  }

  private def newUserQuery()(implicit database: InMemoryEntityDatabase): DbResultSet.Sync[User] =
    DbResultSet.fromExecutor(database.queryExecutor[User])

  private def triggerLazyFetching(database: InMemoryEntityDatabase): Unit = {
    DbResultSet
      .fromExecutor(database.queryExecutor[User])
      .data() // ensure lazy fetching gets triggered (if any)
  }

  private def createUser(id: Long = -1, loginName: String, name: String): User = {
    testUser.copy(
      idOption = Some(if (id == -1) EntityModification.generateRandomId() else id),
      loginName = loginName,
      name = name
    )
  }

  private class FakeEntitiesFetcher extends EntitiesFetcher {
    private val users: mutable.Set[User] = mutable.Set()

    override def fetch[E <: Entity](entityType: EntityType[E]) = entityType match {
      case User.Type => users.toVector.asInstanceOf[Seq[E]]
      case _         => Seq()
    }

    def setUsers(users: User*): Unit = {
      this.users.clear()
      this.users ++= users
    }
  }
}
