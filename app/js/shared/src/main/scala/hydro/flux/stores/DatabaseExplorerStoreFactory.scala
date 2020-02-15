package hydro.flux.stores

import hydro.flux.stores.DatabaseExplorerStoreFactory.State
import hydro.models.access.JsEntityAccess
import hydro.models.modification.EntityModification
import hydro.models.modification.EntityType
import hydro.models.Entity

import scala.async.Async.async
import scala.async.Async.await
import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

final class DatabaseExplorerStoreFactory(implicit entityAccess: JsEntityAccess) extends StoreFactory {

  // **************** Public API **************** //
  def get(entityType: EntityType.any): Store = getCachedOrCreate(entityType)

  // **************** Implementation of base class methods and types **************** //
  override def createNew(input: Input): Store = new Store(input)

  /* override */
  protected type Input = EntityType.any

  /* override */
  final class Store(entityType: EntityType.any) extends AsyncEntityDerivedStateStore[State] {
    // **************** Implementation of base class methods **************** //
    override protected def calculateState(): Future[State] = async {
      val allEntities = await(entityAccess.newQuery()(entityType).data())
      State(allEntities = allEntities)
    }

    override protected def modificationImpactsState(
        entityModification: EntityModification,
        state: State,
    ): Boolean = {
      entityModification.entityType == entityType
    }
  }
}

object DatabaseExplorerStoreFactory {
  case class State(allEntities: Seq[Entity])
}
