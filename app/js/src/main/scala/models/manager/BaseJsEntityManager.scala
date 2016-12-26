package models.manager

import api.ScalaJsApi.EntityType
import models.access.{EntityModification, RemoteDatabaseProxy}

import scala.collection.immutable.Seq
import scala.util.Random

abstract class BaseJsEntityManager[E <: Entity : EntityType](implicit database: RemoteDatabaseProxy) extends EntityManager[E] {

  // **************** Implementation of EntityManager ****************//
  // TODO: Remove these
  final def add(entity: E): E = {
    require(entity.idOption.isEmpty, entity)

    val id = Random.nextLong
    val entityWithId = entity.withId(id).asInstanceOf[E]
    database.persistModifications(Seq(EntityModification.Add(entityWithId)))
    entityWithId
  }

  final def delete(entity: E): Unit = {
    require(entity.idOption.isDefined, entity)

    database.persistModifications(Seq(EntityModification.Remove[E](entity.id)))
  }

  override final def findById(id: Long): E = {
    database.newQuery().findOne("id" -> id.toString).get
  }

  override final def fetchAll(): Seq[E] = {
    database.newQuery().data()
  }
}
