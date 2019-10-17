package services

import javax.inject._
import java.time.Instant

import akka.stream.scaladsl.Source
import dao.PostDao
import models.{Post, User}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostService @Inject()(postDao: PostDao)(implicit val ec: ExecutionContext) {
  def getPosts() = postDao.getPosts()


  def addUserToGroup(userId: Long, groupId: Long) = {
    postDao.addUserToGroup(userId, groupId)
  }

  def getUserGroups(userId: Long) = postDao.getUserGroups(userId)

}
