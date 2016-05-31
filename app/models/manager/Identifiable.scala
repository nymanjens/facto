package models.manager

//
///**
//  * Base trait to define a model having an ID (i.e.: Entity).
//  * The ID is defined as a type alias as it needs to
//  * be accessed by ActiveSlick via type projection when mapping to databse tables.
//  */
//trait Identifiable[E <: Identifiable[E]] {
//
//  /**
//    * The Entity ID wrapped in an Option.
//    * Expected to be None when Entity not yet persisted, otherwise Some[Id]
//    */
//  def id: Option[Long]
//
//  /**
//    * Provide the means to assign an ID to the entity
//    * @return A copy of this Entity with an ID.
//    */
//  def withId(id: Long): E
//}

import models.activeslick.{Identifiable => ActiveSlickIdentifiable}

trait Identifiable[E <: Identifiable[E]] extends ActiveSlickIdentifiable[E]
