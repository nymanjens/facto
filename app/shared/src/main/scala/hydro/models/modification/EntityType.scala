package hydro.models.modification

import hydro.models.Entity

import scala.reflect.ClassTag

/** Enumeration of all entity types that are transfered between server and client. */
final class EntityType[E <: Entity](val entityClass: Class[E]) {
  type get = E

  def checkRightType(entity: Entity): get = {
    require(
      entity.getClass == entityClass,
      s"Got entity of type ${entity.getClass}, but this entityType requires $entityClass",
    )
    entity.asInstanceOf[E]
  }

  lazy val name: String = entityClass.getSimpleName + "Type"
  override def toString = name
}
object EntityType {
  type any = EntityType[_ <: Entity]

  def apply[E <: Entity]()(implicit classTag: ClassTag[E]): EntityType[E] =
    new EntityType[E](classTag.runtimeClass.asInstanceOf[Class[E]])
}
