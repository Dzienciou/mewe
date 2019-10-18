package utils

import play.api.libs.functional.syntax._
import models.{Post, RawPost, User}
import play.api.libs.json.{JsPath, Json, OFormat, Reads, Writes}

object JsonUtils {
  implicit val userFmt: OFormat[User] = Json.format[User]
  implicit val postFmt: OFormat[Post] = Json.format[Post]


  implicit val rawPostWrites: Writes[RawPost] = Json.writes[RawPost]
  implicit val rawPostReads: Reads[RawPost] = (
    (JsPath \ "_id" \ "$oid").read[String] and
      (JsPath \ "created").read[Long] and
      (JsPath \ "content").read[String] and
      (JsPath \ "groupId").read[Long] and
      (JsPath \ "userId").read[Long]
    )(RawPost.apply _)
}
