package com.demo.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.demo.file.client.model.FileResponse
import com.demo.file.client.util.ConfigUtil
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, enrichAny}

import java.nio.file.{Files, Paths}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


class FileClientServerIntegrationTest extends AnyWordSpec with Matchers with ScalatestRouteTest with BeforeAndAfter {

  val requestId = "t_e_s_t_file_dont_use"
  val fileDirUrl = s"${ConfigUtil.centralDirectory}\\${requestId}.${ConfigUtil.defaultFileExtension}"
  val url = s"http://localhost:9090/api/client/get-or-create/${requestId}"

  val responseBody: JsValue = FileResponse("test1", "test1_random").toJson
  val request: HttpRequest = HttpRequest(
    method = HttpMethods.GET,
    uri = url
  )

  override protected def beforeAll(): Unit = {
    println("Deleting test file before test : " + Files.deleteIfExists(Paths.get(fileDirUrl)))
  }

  override protected def afterAll(): Unit = {
    println("#Failesafe Deleting test file after test : " + Files.deleteIfExists(Paths.get(s"${ConfigUtil.centralDirectory}\\${requestId}")))
  }

  "File Client Server" should {

    "return 202 - Accepted with empty body for successful file creation" in {
      val futureRequest = Http().singleRequest(request)

      val response = Await.result(futureRequest, 10.seconds)
      response.status shouldEqual StatusCodes.Accepted
    }

    "return 200-Ok with fileContent for successful file creation" in {
      val futureRequest = Http().singleRequest(request)

      val response = Await.result(futureRequest, 10.seconds)
      response.status shouldEqual StatusCodes.OK
    }
  }
}
