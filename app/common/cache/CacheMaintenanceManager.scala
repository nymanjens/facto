package common.cache

import scala.collection.JavaConverters._

import java.util.List
import java.util.concurrent.CopyOnWriteArrayList

import common.cache.CacheMaintenanceManager.DoNothing.doNothing
import models.manager.Entity

object CacheMaintenanceManager {

  private val doMaintenanceFunctions: List[() => Unit] = new CopyOnWriteArrayList
  private val verifyConsistencyFunctions: List[() => Unit] = new CopyOnWriteArrayList
  private val invalidateCacheFunctions: List[Entity[_] => Unit] = new CopyOnWriteArrayList

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
