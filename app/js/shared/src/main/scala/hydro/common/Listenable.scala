package hydro.common

import hydro.common.Annotations.visibleForTesting
import hydro.common.Listenable.FlatMappedListenable
import hydro.common.Listenable.Listener

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Success

sealed trait Listenable[T] {
  def get: T
  def registerListener(listener: Listener[T]): Unit
  def deregisterListener(listener: Listener[T]): Unit

  final def map[U](mappingFunction: T => U): Listenable[U] = {
    flatMap(mappingFunction = t => Listenable.fixed(mappingFunction(t)))
  }
  final def flatMap[U](mappingFunction: T => Listenable[U]): Listenable[U] = {
    new FlatMappedListenable(this, mappingFunction)
  }
}

object Listenable {

  def fixed[T](value: T): Listenable[T] = new Listenable[T] {
    override def get: T = value
    override def registerListener(listener: Listener[T]): Unit = {}
    override def deregisterListener(listener: Listener[T]): Unit = {}
  }

  def mergeWith[T](mergeFunc: (T, T) => T)(l1: Listenable[T], l2: Listenable[T]): Listenable[T] = {
    new MergedListenable(mergeFunc, l1, l2)
  }

  def fromFuture[T](future: Future[T]): Listenable[Option[T]] = future.value match {
    case Some(Success(value)) => fixed(Some(value))
    case _ =>
      val result = WritableListenable[Option[T]](None)
      future.map(value => result.set(Some(value)))
      result
  }

  trait Listener[T] {
    def onChange(newValue: T): Unit
  }

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

    @visibleForTesting private[common] def hasListeners: Boolean = listeners.nonEmpty
  }
  object WritableListenable {
    def apply[T](value: T): WritableListenable[T] = new WritableListenable[T](value)
  }

  final class ListenableMap[K, V] extends Listenable[Map[K, V]] {
    private var delegateMap: mutable.Map[K, V] = mutable.Map()
    private var listeners: Seq[Listener[Map[K, V]]] = Seq()

    override def get: Map[K, V] = delegateMap.toMap
    override def registerListener(listener: Listener[Map[K, V]]): Unit = {
      listeners = listeners :+ listener
    }
    override def deregisterListener(listener: Listener[Map[K, V]]): Unit = {
      listeners = listeners.filter(_ != listener)
    }

    def contains(key: K): Boolean = delegateMap contains key
    def put(key: K, value: V): Unit = {
      val previousValue: Option[V] = delegateMap.put(key, value)
      if (previousValue != Some(value)) {
        val newValue = get
        listeners.foreach(_.onChange(newValue))
      }
    }
    def apply(key: K): V = delegateMap(key)
  }
  object ListenableMap {
    def apply[K, V](): ListenableMap[K, V] = new ListenableMap[K, V]
  }

  private final class FlatMappedListenable[T, U](origin: Listenable[T], mappingFunction: T => Listenable[U])
      extends Listenable[U] {
    private var lastListenable: Option[Listenable[U]] = None
    private var lastValue: Option[U] = None
    private var listeners: Seq[Listener[U]] = Seq()

    override def get: U = lastValue getOrElse mappingFunction(origin.get).get
    override def registerListener(listener: Listener[U]): Unit = {
      if (listeners.isEmpty) {
        lastListenable = Some(mappingFunction(origin.get))
        lastValue = lastListenable.map(_.get)
        origin.registerListener(OriginListener)
        lastListenable.get.registerListener(MappedListenableListener)
      }
      listeners = listeners :+ listener
    }
    override def deregisterListener(listener: Listener[U]): Unit = {
      listeners = listeners.filter(_ != listener)
      if (listeners.isEmpty) {
        origin.deregisterListener(OriginListener)
        if (lastListenable.isDefined) {
          lastListenable.get.deregisterListener(MappedListenableListener)
        }
        lastListenable = None
        lastValue = None
      }
    }

    private object OriginListener extends Listener[T] {
      override def onChange(newOriginValue: T): Unit = {
        val newListenable = mappingFunction(newOriginValue)
        val newValue = newListenable.get

        lastListenable match {
          case Some(`newListenable`) => // Listenable didn't change --> do nothing
          case Some(oldListenable) =>
            oldListenable.deregisterListener(MappedListenableListener)
            newListenable.registerListener(MappedListenableListener)
          case None =>
            newListenable.registerListener(MappedListenableListener)
        }

        if (lastValue != Some(newValue)) {
          listeners.foreach(_.onChange(newValue))
        }

        lastListenable = Some(newListenable)
        lastValue = Some(newValue)
      }
    }
    private object MappedListenableListener extends Listener[U] {
      override def onChange(newValue: U): Unit = {
        if (lastValue != Some(newValue)) {
          listeners.foreach(_.onChange(newValue))
        }
        lastValue = Some(newValue)
      }
    }
  }

  private final class MergedListenable[T](mergeFunc: (T, T) => T, l1: Listenable[T], l2: Listenable[T])
      extends Listenable[T] {
    private var listeners: Seq[Listener[T]] = Seq()

    override def get: T = mergeFunc(l1.get, l2.get)
    override def registerListener(listener: Listener[T]): Unit = {
      if (listeners.isEmpty) {
        l1.registerListener(OriginListener)
        l2.registerListener(OriginListener)
      }
      listeners = listeners :+ listener
    }
    override def deregisterListener(listener: Listener[T]): Unit = {
      listeners = listeners.filter(_ != listener)
      if (listeners.isEmpty) {
        l1.deregisterListener(OriginListener)
        l2.deregisterListener(OriginListener)
      }
    }

    private object OriginListener extends Listener[T] {
      override def onChange(newOriginValue: T): Unit = {
        val newValue = mergeFunc(l1.get, l2.get)
        listeners.foreach(_.onChange(newValue))
      }
    }
  }
}
