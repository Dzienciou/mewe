package models

import java.time.Instant

case class Post(id: String, created: Instant, content: String, groupId: Long, author: UserInfo)

case class RawPost(id: String, created: Long, content: String, groupId: Long, userId: Long)