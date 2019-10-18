package services

import javax.inject._
import dao.PostDao
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}
import models.{Post, RawPost}
import play.api.libs.json.{JsValue, Json}
import utils.{Merge, MergeSortedN}
import utils.JsonUtils._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostService @Inject()(postDao: PostDao)(implicit val ec: ExecutionContext, implicit val system : ActorSystem) {

  implicit val mat: Materializer = ActorMaterializer()
  implicit val orderingPosts: Ordering[RawPost] = (x: RawPost, y: RawPost) => if (y == x) 0 else
  if (y.created > x.created) 1 else -1

  def addPost(content: String, groupId: Long, userId: Long) = {
    postDao.getUserGroups(userId).flatMap( userGroups =>
      if ( userGroups.contains(groupId))
        postDao.addPost(content: String, groupId: Long, userId: Long).map(Some(_))
      else
        Future(None)
    )

  }

  def getPosts(groupId: Long, userId: Long) = {
    postDao.getUserGroups(userId).flatMap( userGroups =>
      if ( userGroups.contains(groupId))
        postDao.getPosts(groupId).flatMap(_.runWith(Sink.seq)).map(Some(_))
      else
        Future(None)
    )

    }

  def getAllPosts(userId: Long) = {
    postDao.getAllPostsAsSources(userId)
      .map(sources => Merge.mergeSortedN(sources))
  }

  def addUserToGroup(userId: Long, groupId: Long) = {
    postDao.addUserToGroup(userId, groupId)
  }

  def getUserGroups(userId: Long) = postDao.getUserGroups(userId)

}
