package utils

import models.{Post, RawPost, User}
import play.api.libs.json.{Json, OFormat}

object JsonUtils {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val postFmt: OFormat[Post] = Json.format[Post]
  implicit val rawPostFmt: OFormat[RawPost] = Json.format[RawPost]
}
