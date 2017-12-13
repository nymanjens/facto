package jsfacades

import jsfacades.LokiJs.Filter

import scala.collection.immutable.Seq
import scala2js.Converters._
import scala2js.Scala2Js

object LokiJsImplicits {
  implicit class KeyWrapper[E, V: Scala2Js.Converter](key: Scala2Js.Key[V, E]) {
    def isEqualTo(value: V): Filter[E] = Filter.equal(key, value)
    def isNotEqualTo(value: V): Filter[E] = Filter.notEqual(key, value)
    def <(value: V): Filter[E] = Filter.lessThan(key, value)
    def >(value: V): Filter[E] = Filter.greaterThan(key, value)
    def >=(value: V): Filter[E] = Filter.greaterOrEqualThan(key, value)
    def isAnyOf(values: Seq[V]): Filter[E] = Filter.anyOf(key, values)
    def isNoneOf(values: Seq[V]): Filter[E] = Filter.noneOf(key, values)
  }

  implicit class StringKeyWrapper[E](key: Scala2Js.Key[String, E]) {
    def containsIgnoreCase(substring: String): Filter[E] = Filter.containsIgnoreCase(key, substring)
    def doesntContainIgnoreCase(substring: String): Filter[E] =
      Filter.doesntContainIgnoreCase(key, substring)
  }

  implicit class SeqKeyWrapper[E, V: Scala2Js.Converter](key: Scala2Js.Key[Seq[V], E]) {
    def contains(value: V): Filter[E] = Filter.seqContains(key, value)
    def doesntContain(value: V): Filter[E] = Filter.seqDoesntContain(key, value)
  }

  implicit class FilterWrapper[E](thisFilter: Filter[E]) {
    def ||(otherFilter: Filter[E]): Filter[E] = Filter.or(thisFilter, otherFilter)
    def &&(otherFilter: Filter[E]): Filter[E] = Filter.and(thisFilter, otherFilter)
  }
}
