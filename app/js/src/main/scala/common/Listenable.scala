package common

import scala.collection.immutable.Seq
import common.Listenable.Listener

trait Listenable[T] {
  def get: T
  def registerListener(listener: Listener[T]): Unit
  def deregisterListener(listener: Listener[T]): Unit
}

object Listenable {
  final class WritableListenable[T](initialValue: T) extends Listenable[T] {
    private var value: T = initialValue
    private var listeners: Seq[Listener[T]] = Seq()

    override def get: T = value
    override def registerListener(listener: Listener[T]): Unit = {
      listeners = listeners :+ listener
    }
    override def deregisterListener(listener: Listener[T]): Unit = {
      listeners = listeners.filter(_ != listener)
    }

    def set(newValue: T): Unit = {
      val oldValue = value
      if (oldValue != newValue) {
        value = newValue
        listeners.foreach(_.onChange(newValue))
      }
    }
  }
  object WritableListenable {
    def apply[T](value: T): WritableListenable[T] = new WritableListenable[T](value)
  }

  trait Listener[T] {
    def onChange(newValue: T): Unit
  }
}
