package com.demo.file.client.util

import akka.actor.Scheduler
import com.typesafe.scalalogging.LazyLogging

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

class RateLimitChecker(scheduler: Scheduler)(implicit val executionContext: ExecutionContext) extends LazyLogging{

  val map = RateLimitChecker.concurrentMap

  private val task: Runnable = () => {
    logger.info("Cache clear scheduled...!")
    if (map.nonEmpty) map.clear()
  }

  scheduler.scheduleAtFixedRate(ConfigUtil.cacheClearDelay.seconds, ConfigUtil.cacheClearDelay.seconds)(task)

  private def validateAndUpdateResourceRate(resource: String, rate: Int): Int={
    map.get(resource) match {
      case Some(requestRate) => map.put(resource, requestRate + rate); (requestRate + rate)
      case _=> map.put(resource, 0); 0
    }
  }

  def incrementAndGet(resource: String) = validateAndUpdateResourceRate(resource, 1) + 1
  def decrementAndGet(resource: String) = validateAndUpdateResourceRate(resource, -1) - 1

}

object RateLimitChecker{
  val concurrentMap: concurrent.Map[String, Int] = new ConcurrentHashMap[String, Int].asScala
}
