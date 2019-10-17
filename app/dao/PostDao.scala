package dao

import java.time.Instant
import java.util.Date

import javax.inject._
import models.{Post, User}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter, Macros}
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}

class PostDao @Inject() (
                         components: ControllerComponents,
                         val reactiveMongoApi: ReactiveMongoApi,
                       ) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents {

  implicit def ec: ExecutionContext = components.executionContext

  def addPost(content: String, groupId: Long, userId: Long) = {
    val collection = database.map(_.collection[JSONCollection](groupCollectionName(groupId)))
    collection.foreach(_.indexesManager.create(Index(Seq("crerated" -> IndexType.Descending))))
      collection.flatMap(
      _.insert(
        Json.obj(
          "content" -> content,
          "groupId" -> groupId,
          "userId" -> userId,
          "created" -> Instant.now().toEpochMilli
        )
      )
    )
  }

  def getPosts(groupId: Long): Future[JSONCollection] =
    database.map(_.collection[JSONCollection](groupCollectionName(groupId))).flatMap(
      _.find(Json.obj()).sort(Json.obj("created" -> -1)).cursor[RawPost]
    )

  def addUserToGroup(userId: Long, groupId: Long) = {
    val collection = database.map(_.collection[BSONCollection]("groups"))
    collection.flatMap(_.findAndUpdate(
      BSONDocument("userId" -> userId),
      BSONDocument("$addToSet" -> BSONDocument("groups" -> groupId)),
      upsert = true
    ))
  }

  def getUserGroups(userId: Long) = {
    val collection = database.map(_.collection[JSONCollection]("groups"))
    collection.flatMap(_.find(
      Json.obj("userId" -> userId),
      Json.obj("groups" -> 1)
    ).one[JsObject]).map(_.flatMap(js => (js \ "groups").asOpt[List[Long]]))
  }

  def groupCollectionName(groupId: Long) = s"group${groupId}"

}