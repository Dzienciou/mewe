package services

import javax.inject._
import dao.PostDao
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}
import models.{Post, RawPost}
import play.api.libs.json.{JsValue, Json}
import utils.MergeSortedN
import utils.JsonUtils._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostService @Inject()(postDao: PostDao)(implicit val ec: ExecutionContext, implicit val system : ActorSystem) {
  implicit val mat: Materializer = ActorMaterializer()

  implicit val orderingPosts: Ordering[RawPost] = (x: RawPost, y: RawPost) => if (y == x) 0 else
  if (y.created > x.created) 1 else -1

  def mergeSortedN[T: Ordering](sources: immutable.Seq[Source[T, _]]): Source[T, NotUsed] = {
    val source = sources match {
      case immutable.Seq()   => Source.empty[T]
      case immutable.Seq(s1: Source[T, _]) => s1.mapMaterializedValue(_ => NotUsed)
      case s1 +: s2 +: ss   => Source.combine(s1, s2, ss: _*)(new MergeSortedN[T](_))
    }
    source
  }

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
      .map(sources => mergeSortedN(sources))
  }

  def addUserToGroup(userId: Long, groupId: Long) = {
    postDao.addUserToGroup(userId, groupId)
  }

  def getUserGroups(userId: Long) = postDao.getUserGroups(userId)

}
