package models.access

import scala.collection.immutable.Seq

trait DbQueryExecutor[E] {
  def data(dbQuery: DbQuery[E]): Seq[E]
  def count(dbQuery: DbQuery[E]): Int
}
