package services

import java.time.Instant

import javax.inject._
import dao.PostDao
import akka.actor.ActorSystem
import akka.stream._
import models.{Post, RawPost}
import utils.Merge
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostService @Inject()(postDao: PostDao)(implicit val ec: ExecutionContext, implicit val system: ActorSystem) {

  implicit val mat: Materializer = ActorMaterializer()
  implicit val orderingPosts: Ordering[RawPost] = (x: RawPost, y: RawPost) =>
    if (y == x) 0 else if (y.created > x.created) 1 else -1

  def addPost(content: String, groupId: Long, userId: Long) = {
    postDao
      .getUserGroups(userId)
      .flatMap(
        userGroups =>
          if (userGroups.contains(groupId))
            postDao.addPost(content: String, groupId: Long, userId: Long).map(Some(_))
          else
            Future(None)
      )

  }

  def getPosts(groupId: Long, userId: Long) = {
    postDao
      .getUserGroups(userId)
      .flatMap(
        userGroups =>
          if (userGroups.contains(groupId))
            postDao
              .getPosts(groupId)
              .map(Some(_))
          else
            Future(None)
      )
  }

  def getAllPosts(userId: Long) = {
    postDao
      .getAllPostsAsSources(userId)
      .map(sources => Merge.mergeSortedN(sources))
  }

  def addUserToGroup(userId: Long, groupId: Long) = {
    postDao.addUserToGroup(userId, groupId)
  }

  def getUserGroups(userId: Long) = postDao.getUserGroups(userId)

  def addUsername(userId: Long, name: String) = postDao.addUsername(userId, name)

  def getUserInfo(userId: Long) = postDao.getUserInfo(userId)

  def fromRaw(raw: RawPost): Future[Post] =
    postDao
      .getUserInfo(raw.userId)
      .map(Post(raw.id, Instant.ofEpochMilli(raw.created), raw.content, raw.groupId, _))

  def fromRawSeq(raws: Seq[RawPost]): Future[Seq[Post]] = Future.sequence(raws.map(fromRaw))
}
