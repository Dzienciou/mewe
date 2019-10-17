package utils

import models.{Post, User}
import play.api.libs.json.{Format, Json}

object JsonUtils {
  implicit val userFmt: Format[User] = Json.format[User]
  implicit val postFmt: Format[Post] = Json.format[Post]
}
