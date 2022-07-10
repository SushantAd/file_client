package com.demo.file.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RejectionHandler
import akka.util.Timeout
import com.demo.file.client.model.{CustomMessage, PathBusyRejection}
import com.demo.file.client.service.FileService
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.demo.file.client.util.{ConfigUtil, RateLimitChecker}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object FileClientServer extends LazyLogging{

  implicit val system: ActorSystem = ActorSystem()
  val rateLimitChecker = new RateLimitChecker(system.scheduler)
  val fileService = new FileService(system, rateLimitChecker)

  def main(args: Array[String]): Unit = {
    val rejectionHandler = RejectionHandler.newBuilder()
      .handle {
        case PathBusyRejection =>
          complete((StatusCodes.TooManyRequests, ""))
        case CustomMessage(statusCode, message) =>
          complete(statusCode,"")
        case _ => complete(StatusCodes.InternalServerError)
      }.result()

    val route =
      handleRejections(rejectionHandler) {
        pathPrefix("api") {
          pathPrefix("client") {
            get {
              path("get-or-create" / Segment) { resourceName =>
                implicit val timeout: Timeout = ConfigUtil.maxTimeout.seconds
                onSuccess(fileService.checkLocalAndUpstreamServer(resourceName)) {
                  case Right(response) => complete(response)
                  case Left(response) => reject(response)
                }
              }
            }
          }
        }
      }

    Http().newServerAt(ConfigUtil.applicationHost, ConfigUtil.applicationPort).bind(route).onComplete {
      case Success(_) => logger.info(s"Listening for requests on http://${ConfigUtil.applicationHost}:${ConfigUtil.applicationPort}")
      case Failure(ex) =>
        logger.info(s"Failed to bind to ${ConfigUtil.applicationHost}:${ConfigUtil.applicationPort}")
        ex.printStackTrace()
    }

  }


}
