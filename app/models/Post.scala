package models

import java.time.Instant

case class Post(id: Option[String], created: Instant, content: String, groupId: Int, author: User)

case class RawPost(id: String, created: Long, content: String, groupId: Long, userId: Long)