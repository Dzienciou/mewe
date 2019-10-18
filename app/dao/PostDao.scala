package dao

import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.inject._
import models.{Post, RawPost, UserInfo}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.akkastream.{State, cursorProducer}
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}

class PostDao @Inject()(
    components: ControllerComponents,
    system: ActorSystem,
    val reactiveMongoApi: ReactiveMongoApi
) extends AbstractController(components)
    with MongoController
    with ReactiveMongoComponents {

  implicit def ec: ExecutionContext            = components.executionContext
  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  def addPost(content: String, groupId: Long, userId: Long) = {
    val collection = database.map(_.collection[JSONCollection](groupCollectionName(groupId)))
    collection.foreach(_.indexesManager.create(Index(Seq("created" -> IndexType.Descending))))
    collection.flatMap(
      _.insert(
        Json.obj(
          "content" -> content,
          "groupId" -> groupId,
          "userId"  -> userId,
          "created" -> Instant.now().toEpochMilli
        )
      )
    )
  }

  def getAllPostsAsSources(userId: Long) =
    getUserGroups(userId).flatMap(groups => Future.sequence(groups.map(getPosts)))

  def getPosts(groupId: Long) =
    database
      .map(_.collection[JSONCollection](groupCollectionName(groupId)))
      .map(_.find(Json.obj()).sort(Json.obj("created" -> -1)).cursor[RawPost]().documentSource())

  def addUserToGroup(userId: Long, groupId: Long) = {
    val collection = database.map(_.collection[BSONCollection]("groups"))
    collection.flatMap(
      _.findAndUpdate(
        BSONDocument("userId"    -> userId),
        BSONDocument("$addToSet" -> BSONDocument("groups" -> groupId)),
        upsert = true
      )
    )
  }

  def getUserGroups(userId: Long) = {
    val collection = database.map(_.collection[JSONCollection]("groups"))
    collection
      .flatMap(
        _.find(
          Json.obj("userId" -> userId),
          Json.obj("groups" -> 1)
        ).one[JsObject]
      )
      .map(_.flatMap(js => (js \ "groups").asOpt[Seq[Long]]).getOrElse(Seq.empty))
  }

  def addUsername(userId: Long, name: String) =
    database
      .map(_.collection[JSONCollection]("users"))
      .flatMap(
        _.findAndUpdate(
          Json.obj("_id"  -> userId),
          Json.obj("$set" -> Json.obj("name" -> name)),
          upsert = true
        )
      )

  def getUserInfo(userId: Long) =
    database
      .map(_.collection[JSONCollection]("users"))
      .flatMap(_.find(Json.obj("_id" -> userId)).one[UserInfo])
      .map(_.getOrElse(UserInfo(userId, s"defaultUsername$userId")))

  def groupCollectionName(groupId: Long) = s"group$groupId"

}
