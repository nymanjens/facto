package flux.stores.entries.factories

import common.GuavaReplacement.ImmutableSetMultimap
import common.testing.TestObjects._
import common.testing.{FakeJsEntityAccess, TestModule}
import models.accounting._
import utest._

import scala.async.Async.{async, await}
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala2js.Converters._

object TagsStoreFactoryTest extends TestSuite {

  override def tests = TestSuite {
    val testModule = new TestModule()
    implicit val entityAccess = testModule.fakeEntityAccess
    val factory: TagsStoreFactory = new TagsStoreFactory()

    "empty result" - async {
      val state = await(factory.get().stateFuture)

      state.tagToTransactionIds ==> ImmutableSetMultimap.of()
    }

    "gives correct results" - async {
      persistTransaction(id = 101, tags = Seq("aa", "bb"))
      persistTransaction(id = 102, tags = Seq("aa"))
      persistTransaction(id = 103, tags = Seq("bb", "cc"))

      val state = await(factory.get().stateFuture)

      state.tagToTransactionIds ==>
        ImmutableSetMultimap
          .builder[TagsStoreFactory.Tag, TagsStoreFactory.TransactionId]()
          .putAll("aa", 101, 102)
          .putAll("bb", 101, 103)
          .putAll("cc", 103)
          .build()
    }
  }

  private def persistTransaction(id: Long, tags: Seq[String])(
      implicit entityAccess: FakeJsEntityAccess): Transaction = {
    val transaction = testTransactionWithIdA.copy(idOption = Some(id), tags = tags)
    entityAccess.addRemotelyAddedEntities(transaction)
    transaction
  }
}
