package com.demo.file.client.model

import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol._


final case class FileResponse(requestId: String, fileContent: String)

object FileResponse {
  implicit val responseFormat: RootJsonFormat[FileResponse] = jsonFormat2(FileResponse.apply)
}
