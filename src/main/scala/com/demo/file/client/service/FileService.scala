package com.demo.file.client.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.demo.file.client.model.{CustomMessage, FileResponse}
import spray.json.enrichAny
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.demo.file.client.util.ConfigUtil
import spray.json.DefaultJsonProtocol._

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.CollectionHasAsScala

class FileService(system: ActorSystem)(implicit ex: ExecutionContext) {

  private implicit val actorSystem: ActorSystem = system

  def checkLocalAndUpstreamServer(fileName: String): Future[Either[CustomMessage, FileResponse]] = {
    val fileServerUrl = ConfigUtil.fileServerUrl
    println(s"fileServerUrl = ${fileServerUrl}")
    val path = s"""C:\\centralDir\\$fileName"""
    val filePath = Paths.get(s"$path.txt")
    checkAndGetFile(filePath) match {
      case Some(fileContent) => Future(Right(FileResponse(fileName, fileContent)))
      case _ => akka.pattern.after(500.millis)(getFileFromServer(fileServerUrl, fileName))
    }
  }

  private def createFile(fileName: String, fileContent: String) = {
    val path = s"""C:\\centralDir2\\$fileName"""
    val resultPath = Files.write(Paths.get(s"$path.txt"), s"${fileContent}".getBytes(StandardCharsets.UTF_8))
    checkAndGetFile(resultPath)
  }

  private def checkAndGetFile(path: Path) = {
    if (Files.exists(path))
      Some(Files.readAllLines(path).asScala.mkString)
    else
      None
  }

  private def getFileFromServer(url: String, requestId: String)(): Future[Either[CustomMessage, FileResponse]] = {
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
            Unmarshal(req.entity).to[FileResponse].flatMap{ fileContent=>
              createFile(requestId, fileContent.fileContent) match {
                case Some(fileCreated) => Future(Right(FileResponse(requestId, fileCreated)))
                case _=>
                  Future(Left(CustomMessage(StatusCodes.Accepted, "Too many requests..retrying!!")))
              }
            }
          case StatusCodes.TooManyRequests =>
            Future(Left(CustomMessage(StatusCodes.Accepted, "Too many requests..retrying!!")))
          case _ => Future(Left(CustomMessage(StatusCodes.InternalServerError, s"Upstream server failed with error: ${req.entity.toString}")))
        }
    }
  }

}
