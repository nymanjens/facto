package models.manager

import api.ScalaJsApi.EntityType
import models.access.{EntityModification, RemoteDatabaseProxy}

import scala.collection.immutable.Seq
import scala.util.Random
import scala2js.Converters._
import scala2js.Scala2Js

abstract class BaseJsEntityManager[E <: Entity](database: RemoteDatabaseProxy) extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  override final def add(entity: E): E = {
    require(entity.idOption.isEmpty, entity)

    val id = Random.nextLong
    val entityWithId = entity.withId(id).asInstanceOf[E]
    database.persistModifications(Seq(EntityModification.Add(entityType, entityWithId)))
    entityWithId
  }

  override final def update(entity: E): E = throw new UnsupportedOperationException(s"Only immutable entities are supported")

  override final def delete(entity: E): Unit = {
    require(entity.idOption.isDefined, entity)

    database.persistModifications(Seq(EntityModification.Remove(entityType, entity.id)))
  }

  /** Returns the entity with given ID or throws an exception. */
  override final def findById(id: Long): E = {
    val result = database.newQuery(entityType).findOne("id" -> id.toString).get
    Scala2Js.toScala[E](result)
  }

  /** Returns all stored entities. */
  override final def fetchAll(): Seq[E] = {
    val result = database.newQuery(entityType).data()
    Scala2Js.toScala[Seq[E]](result)
  }

  // **************** Abstract methods ****************//
  protected def entityType: EntityType

  // **************** Private helper methods ****************//
  implicit private def entityConverter: Scala2Js.Converter[E] = {
    entityTypeToConverter(entityType).asInstanceOf[Scala2Js.Converter[E]]
  }
}