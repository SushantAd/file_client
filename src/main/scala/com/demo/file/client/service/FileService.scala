package com.demo.file.client.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.demo.file.client.model.{CustomMessage, FileResponse}
import spray.json.enrichAny
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.demo.file.client.util.{ConfigUtil, RateLimitChecker}
import com.typesafe.scalalogging.LazyLogging
import spray.json.DefaultJsonProtocol._

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationLong
import scala.jdk.CollectionConverters.CollectionHasAsScala

class FileService(system: ActorSystem, rateLimitChecker: RateLimitChecker)(implicit ex: ExecutionContext) extends LazyLogging{

  private implicit val actorSystem: ActorSystem = system

  def checkLocalAndUpstreamServer(requestId: String): Future[Either[CustomMessage, FileResponse]] = {
    logger.info(s"Request to check local and upstream server for file: ${requestId} started")
    val path= s"${ConfigUtil.centralDirectory}\\$requestId"
    val filePath = Paths.get(s"$path.txt")
    checkAndGetFile(filePath) match {
      case Some(fileContent) => Future(Right(FileResponse(requestId, fileContent)))
      case _ => getFileWithThrottle(requestId)
    }
  }

  private def getFileWithThrottle(requestId: String): Future[Either[CustomMessage, FileResponse]] ={
    if (rateLimitChecker.incrementAndGet(requestId) > ConfigUtil.maxRequest) {
      rateLimitChecker.decrementAndGet(requestId)
      akka.pattern.after(ConfigUtil.fileServerMaxDelay.millis)(getFileFromServer(requestId))
    } else {
      rateLimitChecker.decrementAndGet(requestId)
      getFileFromServer(requestId)
    }
  }

  private def createFile(fileName: String, fileContent: String) = {
    val path= s"${ConfigUtil.centralDirectory}\\$fileName"
    val resultPath = Files.write(Paths.get(s"$path.txt"), s"${fileContent}".getBytes(StandardCharsets.UTF_8))
    checkAndGetFile(resultPath)
  }

  private def checkAndGetFile(path: Path) = {
    if (Files.exists(path))
      Some(Files.readAllLines(path).asScala.mkString)
    else
      None
  }

  private def getFileFromServer(requestId: String): Future[Either[CustomMessage, FileResponse]] = {
    val url = ConfigUtil.fileServerUrl

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = url,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Map("requestId" -> requestId).toJson.toString()
      )
    )

    Http().singleRequest(request).flatMap {
      req =>
        req.status match {
          case StatusCodes.OK if req.entity.contentType == ContentTypes.`application/json` =>
            Unmarshal(req.entity).to[FileResponse].flatMap{ file=>
              createFile(requestId, file.fileContent) match {
                case Some(fileCreated) => logger.info(s"File created for requestId: ${requestId}")
                case _=> logger.error(s"File not created, please check logs.")
              }
              Future(Left(CustomMessage(StatusCodes.Accepted, "")))
            }
          case StatusCodes.TooManyRequests =>
            logger.error(s"Server rate limit reached, too many requests for requestId: ${requestId}..Retrying")
            getFileWithThrottle(requestId)
            Future(Left(CustomMessage(StatusCodes.Accepted, "Too many requests")))
          case _ =>
            logger.error("Upstream server failed, please check logs")
            Future(Left(CustomMessage(StatusCodes.InternalServerError, s"Upstream server failed with error: ${req.entity.toString}")))
        }
    }
  }

}
