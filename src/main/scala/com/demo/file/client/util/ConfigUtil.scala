package com.demo.file.client.util

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {

  private val conf: Config = ConfigFactory.load()

  val applicationHost : String = conf.getString("application.host")
  val applicationPort : Int = conf.getInt("application.port")

  val fileServerUrl: String = conf.getString("file_server.url")
  val fileServerMaxDelay: Long = conf.getInt("file_server.maxDelay")

  val maxRequest: Int = conf.getInt("request.max-request")
  val maxTimeout: Int = conf.getInt("request.max-timeout")

  val centralDirectory: String = conf.getString("file_store.central-directory")
  val defaultFileExtension: String = conf.getString("file_store.default-extension")

  val cacheClearDelay: Int = conf.getInt("rate_limiter.cache.clear.delay")
}
