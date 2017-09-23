package flux.stores.entries

import common.GuavaReplacement.ImmutableSetMultimap
import common.accounting.Tags
import common.testing.TestObjects._
import common.testing.{FakeRemoteDatabaseProxy, TestModule}
import models.accounting._
import utest._

import scala.collection.immutable.Seq
import scala2js.Converters._

object TagsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val database = testModule.fakeRemoteDatabaseProxy
    val factory: TagsStoreFactory = new TagsStoreFactory()

    "empty result" - {
      factory.get().state.tagToTransactionIds ==> ImmutableSetMultimap.of()
    }

    "gives correct results" - {
      persistTransaction(id = 101, tags = Seq("aa", "bb"))
      persistTransaction(id = 102, tags = Seq("aa"))
      persistTransaction(id = 103, tags = Seq("bb", "cc"))

      factory.get().state.tagToTransactionIds ==>
        ImmutableSetMultimap
          .builder[TagsStoreFactory.Tag, TagsStoreFactory.TransactionId]()
          .putAll("aa", 101, 102)
          .putAll("bb", 101, 103)
          .putAll("cc", 103)
          .build()
    }
  }

  private def persistTransaction(id: Long, tags: Seq[String])(
      implicit database: FakeRemoteDatabaseProxy): Transaction = {
    val transaction = testTransactionWithIdA.copy(idOption = Some(id), tags = tags)
    database.addRemotelyAddedEntities(transaction)
    transaction
  }
}
