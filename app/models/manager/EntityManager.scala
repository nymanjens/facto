package models.manager

import scala.collection.immutable.Seq

trait EntityManager[E <: Identifiable[E]] {

  // ********** Management methods ********** //
  def initialize(): Unit = {}
  def verifyConsistency(): Unit = {}
  def createSchema: Unit
  def tableName: String

  // ********** Mutators ********** //
  def add(entity: E): E
  def update(entity: E): E
  def delete(entity: E): Unit

  // ********** Getters ********** //
  def findById(id: Long): E
  def fetchFromAll[R](calculateResult: Stream[E] => R): R
  def fetchAll(selection: Stream[E] => Stream[E] = s => s): List[E] = fetchFromAll(stream => selection(stream).toList)
  def count(predicate: E => Boolean = _ => true): Int = fetchFromAll(_.count(predicate))
}

object EntityManager {
  def caching[E <: Identifiable[E]](delegate: EntityManager[E]): EntityManager[E] =
    new CachingEntityManager(delegate)
}