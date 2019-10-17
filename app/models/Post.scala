package models

import java.time.Instant

case class Post(id: Int, created: Instant, content: String, groupId: Int, author: User)