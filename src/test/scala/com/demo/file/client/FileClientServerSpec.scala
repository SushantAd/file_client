package com.demo.file.client

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.demo.file.client.model.FileResponse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol._
import spray.json.{JsValue, enrichAny}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration.DurationInt

/*
UT based on -> https://doc.akka.io/docs/akka-http/current/routing-dsl/testkit.html
 */

class FileClientServerSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(10.seconds)

  val url = "/api/client/get-or-create"

  val requestBody = "test1"

  val responseBody: JsValue = FileResponse("test1", "test1_random").toJson

  val wrongRequest = Map("wrong"-> "request").toJson


  val testRoute =
    pathPrefix("api") {
      pathPrefix("client") {
        get {
          path("get-or-create") {
            complete(responseBody)
          }
        }
      }
    }

  val rejectRoute =
    pathPrefix("api") {
      pathPrefix("client") {
        get {
          path("get-or-create") {
            complete(StatusCodes.Accepted)
          }
        }
      }
    }

  val errorRoute =
    pathPrefix("api") {
      pathPrefix("client") {
        get {
          path("get-or-create") {
            complete(StatusCodes.InternalServerError)
          }
        }
      }
    }

  "File Client Server" should{

    "return 200 with fileContent for successful file creation" in{
      Get(url) ~> testRoute ~> check{
        entityAs[FileResponse].toJson shouldEqual responseBody
      }
    }

    "return accepted if server times out due to, too many requests" in{
      Get(url) ~> rejectRoute ~> check{
        status shouldEqual StatusCodes.Accepted
      }
    }

    "return 500 internal server error for wrong request" in{
      Get(url) ~> errorRoute ~> check{
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }

}
