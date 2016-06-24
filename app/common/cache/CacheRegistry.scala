package common.cache

import scala.collection.JavaConverters._

import java.util.List
import java.util.concurrent.CopyOnWriteArrayList

import common.cache.CacheRegistry.DoNothing.doNothing
import models.manager.Entity

/**
  * Static registry for all kinds of caches inside Facto. It defines some hooks that caches can opt-into and that
  * clients can call.
  */
object CacheRegistry {

  private val doMaintenanceFunctions: List[() => Unit] = new CopyOnWriteArrayList
  private val verifyConsistencyFunctions: List[() => Unit] = new CopyOnWriteArrayList
  private val invalidateCacheFunctions: List[Entity[_] => Unit] = new CopyOnWriteArrayList

  /** Registers given functions so that they are called when their respective events are triggered. */
  def registerCache(doMaintenance: () => Unit = doNothing,
                    verifyConsistency: () => Unit = doNothing,
                    invalidateCache: Entity[_] => Unit = doNothing): Unit = {
    doMaintenanceFunctions.add(doMaintenance)
    verifyConsistencyFunctions.add(verifyConsistency)
    invalidateCacheFunctions.add(invalidateCache)
  }

  /** Performs regular maintenance on all caches and throws an exception if there is a consistency problem. */
  def doMaintenanceAndVerifyConsistency(): Unit = {
    for (doMaintenance <- doMaintenanceFunctions.asScala) {
      doMaintenance()
    }
    for (verifyConsistency <- verifyConsistencyFunctions.asScala) {
      verifyConsistency()
    }
  }

  /** Signals to all caches that given entity was updated and should be removed from all caches. */
  def invalidateCachesWhenUpdated(entity: Entity[_]): Unit = {
    for (invalidateCache <- invalidateCacheFunctions.asScala) {
      invalidateCache(entity)
    }
  }

  object DoNothing {
    def doNothing(): Unit = {}
    def doNothing(entity: Entity[_]): Unit = {}
  }
}
