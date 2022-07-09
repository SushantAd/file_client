package com.demo.file.client.util

import com.typesafe.config.{Config, ConfigFactory}

object ConfigUtil {

  private val conf: Config = ConfigFactory.load()

  val fileServerUrl: String = conf.getString("file_server.url")

  val maxRequest: Int = conf.getInt("request.max-request")
  val maxTimeout: Int = conf.getInt("request.max-timeout")

  val centralDirectory: String = conf.getString("file_store.central-directory")
  val defaultFileExtension: String = conf.getString("file_store.default-extension")

}
