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
import com.demo.file.client.util.ConfigUtil

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object FileClientServer {

  implicit val system: ActorSystem = ActorSystem()
  val fileService = new FileService(system)

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
          pathPrefix("server") {
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

    Http().newServerAt("127.0.0.1", 9090).bind(route).onComplete {
      case Success(_) => println("Listening for requests on http://127.0.0.1:9090")
      case Failure(ex) =>
        println("Failed to bind to 127.0.0.9090")
        ex.printStackTrace()
    }

  }


}
