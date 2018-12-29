package app.models.access

import app.common.testing.TestObjects._
import app.common.testing._
import app.models.Entity
import app.models.access.DbQuery.Sorting
import app.models.access.DbQueryImplicits._
import app.models.access.InMemoryEntityDatabase.EntitiesFetcher
import app.models.accounting.Transaction
import app.models.modification.EntityModification
import app.models.modification.EntityType
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class InMemoryEntityDatabaseTest extends HookedSpecification {

  private val entitiesFetcher = new FakeEntitiesFetcher

  val trans1 = createTransaction(day = 1, flow = 3)
  val trans2 = createTransaction(day = 2, flow = 2)
  val trans3 = createTransaction(day = 3, flow = 1)

  "queryExecutor()" in {
    entitiesFetcher.transactions ++= Seq(trans1, trans2, trans3)

    val database = new InMemoryEntityDatabase(entitiesFetcher)

    runTestsAssumingTrans123(database)
  }

  "update()" in {
    "Add" in {
      entitiesFetcher.transactions ++= Seq(trans1, trans2)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[Transaction])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.transactions += trans3
      database.update(EntityModification.Add(trans3))

      runTestsAssumingTrans123(database)
    }

    "Remove" in {
      val trans4 = createTransaction(day = 4, flow = -1)

      entitiesFetcher.transactions ++= Seq(trans1, trans2, trans3, trans4)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[Transaction])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.transactions -= trans4
      database.update(EntityModification.createDelete(trans4))

      runTestsAssumingTrans123(database)
    }

    "Update" in {
      val trans2AtCreate = createTransaction(id = trans2.id, day = 9, flow = 99)

      entitiesFetcher.transactions ++= Seq(trans1, trans2AtCreate, trans3)
      val database = new InMemoryEntityDatabase(entitiesFetcher)
      DbResultSet
        .fromExecutor(database.queryExecutor[Transaction])
        .data() // ensure lazy fetching gets triggered (if any)

      entitiesFetcher.transactions -= trans2AtCreate
      entitiesFetcher.transactions += trans2
      database.update(EntityModification.Update(trans2))

      runTestsAssumingTrans123(database)
    }
  }

  private def runTestsAssumingTrans123(database: InMemoryEntityDatabase) = {
    val executor = database.queryExecutor[Transaction]

    "cached sorting" in {
      DbResultSet
        .fromExecutor(executor)
        .limit(2)
        .sort(Sorting.Transaction.deterministicallyByCreateDate)
        .data() mustEqual Seq(trans1, trans2)
    }
    "cached sorting inverse" in {
      DbResultSet
        .fromExecutor(executor)
        .limit(2)
        .sort(Sorting.Transaction.deterministicallyByCreateDate.reversed)
        .data() mustEqual Seq(trans3, trans2)
    }
    "non-cached sorting" in {
      DbResultSet
        .fromExecutor(executor)
        .limit(2)
        .sort(Sorting.ascBy(ModelFields.Transaction.flowInCents))
        .data() mustEqual Seq(trans3, trans2)
    }
    "filtered" in {
      DbResultSet
        .fromExecutor(executor)
        .filter(ModelFields.Transaction.flowInCents === 200)
        .data() mustEqual Seq(trans2)
    }
  }

  private class FakeEntitiesFetcher extends EntitiesFetcher {
    val transactions: mutable.Set[Transaction] = mutable.Set()

    override def fetch[E <: Entity](entityType: EntityType[E]) = entityType match {
      case EntityType.TransactionType => transactions.toVector.asInstanceOf[Seq[E]]
      case _                          => Seq()
    }
  }
}
