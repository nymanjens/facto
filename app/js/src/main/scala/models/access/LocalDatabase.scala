package models.access

import jsfacades.Loki
import models.manager.{Entity, EntityType, EntityModification}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala2js.Converters._
import scala2js.Scala2Js

private[access] trait LocalDatabase {
  // **************** Getters ****************//
  def newQuery[E <: Entity : EntityType](): Loki.ResultSet[E]
  def getSingletonValue[V](key: SingletonKey[V]): Option[V]
  def isEmpty(): Boolean

  // **************** Setters ****************//
  /** Applies given modification in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def applyModifications(modifications: Seq[EntityModification]): Unit
  def addAll[E <: Entity : EntityType](entities: Seq[E]): Unit
  /** Sets given singleton value in memory but doesn't persist it in the browser's storage (call `save()` to do this). */
  def setSingletonValue[V](key: SingletonKey[V], value: V): Unit
  /** Persists all previously made changes to the browser's storage. */
  def save(): Future[Unit]
  def clear(): Future[Unit]
}

private[access] object LocalDatabase {

  def createFuture(): Future[LocalDatabase] = {
    val lokiDb: Loki.Database = Loki.Database.persistent("facto-db")
    lokiDb.loadDatabase() map (_ => new Impl(lokiDb))
  }

  def createInMemoryForTests(): Future[LocalDatabase] = {
    val lokiDb: Loki.Database = Loki.Database.inMemoryForTests("facto-db")
    lokiDb.loadDatabase() map (_ => new Impl(lokiDb))
  }

  private case class Singleton(key: String, value: js.Any)

  private object Singleton {
    implicit object Converter extends Scala2Js.MapConverter[Singleton] {
      override def toJs(singleton: Singleton) = {
        js.Dictionary[js.Any](
          "key" -> singleton.key,
          "value" -> singleton.value)
      }
      override def toScala(dict: js.Dictionary[js.Any]) = {
        def getRequired[T: Scala2Js.Converter](key: String) = getRequiredValueFromDict[T](dict)(key)
        Singleton(
          key = getRequired[String]("key"),
          value = getRequired[js.Any]("value"))
      }
    }
  }

  private final class Impl(val lokiDb: Loki.Database) extends LocalDatabase {
    val entityCollections: Map[EntityType.any, Loki.Collection[_]] = {
      def getOrAddCollection[E <: Entity](implicit entityType: EntityType[E]): Loki.Collection[E] = {
        // TODO: Add primary indices
        lokiDb.getOrAddCollection[E](s"entities_${entityType.name}")
      }
      for (entityType <- EntityType.values) yield {
        entityType -> getOrAddCollection(entityType)
      }
    }.toMap
    // TODO: Add primary index on key
    val singletonCollection: Loki.Collection[Singleton] = lokiDb.getOrAddCollection[Singleton](s"singletons")

    // **************** Getters ****************//
    override def newQuery[E <: Entity : EntityType](): Loki.ResultSet[E] = {
      entityCollectionForImplicitType.chain()
    }

    override def getSingletonValue[V](key: SingletonKey[V]): Option[V] = {
      implicit val converter = key.valueConverter
      val value = singletonCollection.chain().findOne("key" -> key.name)
      value.map(v => Scala2Js.toScala[V](v.value))
    }

    override def isEmpty(): Boolean = {
      allCollections.toStream.filter(_.chain().count() != 0).isEmpty
    }

    // **************** Setters ****************//
    override def applyModifications(modifications: Seq[EntityModification]): Unit = {
      for (modification <- modifications) {
        modification match {
          case addModification: EntityModification.Add[_] =>
            def add[E <: Entity](modification: EntityModification.Add[E]): Unit = {
              implicit val _ = modification.entityType
              newQuery[E]().findOne("id" -> modification.entity.id.toString) match {
                case Some(entity) => // do nothing
                case None => entityCollectionForImplicitType[E].insert(modification.entity)
              }
            }
            add(addModification)
          case removeModification: EntityModification.Remove[_] =>
            def remove[E <: Entity](modification: EntityModification.Remove[E]): Unit = {
              implicit val _ = modification.entityType
              newQuery[E]().findOne("id" -> modification.entityId.toString) match {
                case Some(entity) => entityCollectionForImplicitType.findAndRemove("id" -> modification.entityId.toString)
                case None => // do nothing
              }
            }
            remove(removeModification)
        }
      }
    }

    override def addAll[E <: Entity : EntityType](entities: Seq[E]): Unit = {
      for (entity <- entities) {
        entityCollectionForImplicitType.insert(entity)
      }
    }

    override def setSingletonValue[V](key: SingletonKey[V], value: V): Unit = {
      implicit val converter = key.valueConverter
      singletonCollection.findAndRemove("key" -> key.name)
      singletonCollection.insert(Singleton(
        key = key.name,
        value = Scala2Js.toJs(value)
      ))
    }

    override def save(): Future[Unit] = {
      lokiDb.saveDatabase()
    }

    override def clear(): Future[Unit] = {
      for (collection <- allCollections) {
        collection.clear()
      }
      lokiDb.saveDatabase()
    }

    // **************** Private helper methods ****************//
    private def allCollections: Seq[Loki.Collection[_]] = entityCollections.values.toList :+ singletonCollection

    private def entityCollectionForImplicitType[E <: Entity : EntityType]: Loki.Collection[E] = {
      entityCollections(implicitly[EntityType[E]]).asInstanceOf[Loki.Collection[E]]
    }
  }
}
