package app.flux.stores.entries.factories

import hydro.common.GuavaReplacement.ImmutableSetMultimap
import hydro.common.testing.FakeJsEntityAccess
import app.common.testing.TestModule
import app.common.testing.TestObjects._
import app.models.accounting._
import utest._

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

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

  private def persistTransaction(id: Long, tags: Seq[String])(implicit
      entityAccess: FakeJsEntityAccess
  ): Transaction = {
    val transaction = testTransactionWithIdA.copy(idOption = Some(id), tags = tags)
    entityAccess.addRemotelyAddedEntities(transaction)
    transaction
  }
}
