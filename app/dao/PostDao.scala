package dao

import javax.inject._
import models.User
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class PostDao @Inject() (
                         components: ControllerComponents,
                         val reactiveMongoApi: ReactiveMongoApi,
                       ) extends AbstractController(components)
  with MongoController with ReactiveMongoComponents {

  implicit def ec: ExecutionContext = components.executionContext

  def getPosts(): Future[JSONCollection] =
    database.map(_.collection[JSONCollection]("group1"))

  def addUserToGroup(userId: Long, groupId: Long) = {
    val collection = database.map(_.collection[BSONCollection]("groups"))
    val selector = BSONDocument("userId" -> userId)
    val mod = BSONDocument("$set" -> BSONDocument("groups" -> groupId))
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
}