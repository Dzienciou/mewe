package controllers

import javax.inject._
import models.Post
import cats.implicits._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.PostService
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class PostController @Inject()(postService: PostService, cc: ControllerComponents)(implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def getPosts(groupId: Long) = Action.async { implicit request =>
    postService.getPosts(groupId).map(c =>
      Ok(Json.toJson(c)))
  }

  def addUserToGroup(id: Long) = Action.async(parse.tolerantJson) { implicit request =>
    parseToken(request)
      .map { token =>
        postService.addUserToGroup(token, id).transformWith {
          case Success(_) => Future(Ok)
          case Failure(ex) => Future(InternalServerError(ex.getMessage))
        }
      } getOrElse Future(BadRequest)
  }

  def getUserGroups() = Action.async(parse.tolerantJson) { implicit request =>
    parseToken(request)
      .map ( token =>
        postService.getUserGroups(token).map(groups =>
          Ok(Json.obj("groups" -> Json.toJson(groups))))
        ) getOrElse Future(BadRequest)
  }

  def addPost(groupId: Long) = Action.async(parse.tolerantJson) { implicit request: Request[JsValue] =>
    ((request.body \ "content" ).asOpt[String], parseToken(request)).mapN( (content, userId) =>
        postService.addPost(content, groupId, userId).transformWith {
          case Success(_) => Future(Ok)
          case Failure(ex) => Future(InternalServerError(ex.getMessage))
        }

    ) getOrElse Future(BadRequest)
  }

  def parseToken(req: Request[JsValue]) =
    req.headers
      .get("Auth-Token")
      .flatMap(_.toLongOption)


}
