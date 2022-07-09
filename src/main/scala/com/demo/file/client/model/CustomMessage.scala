package com.demo.file.client.model

import akka.http.scaladsl.model.{StatusCode, Uri}
import akka.http.scaladsl.server.Rejection

case class CustomMessage(statusCode: StatusCode, message: String) extends Rejection

case object PathBusyRejection extends Rejection
