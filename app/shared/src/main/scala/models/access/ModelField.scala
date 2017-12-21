package models.access

import java.util.Objects

import scala.reflect.ClassTag

/**
  * Represents a field in an model entity.
  *
  * @param name A name for this field that is unique in E
  * @tparam V The type of the values
  * @tparam E The type corresponding to the entity that contains this field
  */
final class ModelField[V, E] private[access] (val name: String, accessor: E => V) {

  def get(entity: E): V = accessor(entity)

  override def equals(any: scala.Any) = {
    any match {
      case that: ModelField[_, _] => this.name == that.name
      case _ => false
    }

  }
  override def hashCode() = Objects.hash(name)
}
