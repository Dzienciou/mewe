package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import javax.inject._
import models.{Post, RawPost}
import cats.implicits._
import play.api.http.ContentTypes
import play.api.libs.{Comet, EventSource}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc._
import services.PostService
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class PostController @Inject()(postService: PostService, system: ActorSystem, cc: ControllerComponents)(
    implicit val ec: ExecutionContext
) extends AbstractController(cc) {

  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)

  def getPosts(groupId: Long) = Action.async { implicit request =>
    val token: Long = parseToken(request).getOrElse(0)
    postService.getPosts(groupId, token).map {
      case Some(source) =>
        Ok.chunked(source
          .mapAsync(10)(postService.fromRaw)
          .via(Flow.fromFunction(Json.toJson(_)))
      )
      case None        => BadRequest(s"User $token is not a member of a group $groupId")
    }
  }

  def getAllUserPosts() = Action.async { implicit request =>
    val token: Long = parseToken(request).getOrElse(0)
    postService
      .getAllPosts(token)
      .map(
        source =>
          Ok.chunked(
              source
                .mapAsync(10)(postService.fromRaw)
                  .via(Flow.fromFunction(Json.toJson(_)))
          )
      )
  }

  def addUserToGroup(id: Long) = Action.async { implicit request =>
    parseToken(request)
      .map { token =>
        postService.addUserToGroup(token, id).transformWith {
          case Success(_)  => Future(Ok)
          case Failure(ex) => Future(InternalServerError(ex.getMessage))
        }
      } getOrElse Future(BadRequest)
  }

  def getUserGroups() = Action.async(parse.tolerantJson) { implicit request =>
    parseToken(request)
      .map(token => postService.getUserGroups(token).map(groups => Ok(Json.obj("groups" -> Json.toJson(groups))))) getOrElse Future(
      BadRequest
    )
  }

  def addPost(groupId: Long) = Action.async(parse.tolerantJson) { implicit request: Request[JsValue] =>
    ((request.body \ "content").asOpt[String], parseToken(request)).mapN(
      (content, token) =>
        postService.addPost(content, groupId, token).map {
          case Some(_) => Ok
          case None    => BadRequest(s"User $token is not a member of a group $groupId")
        }
    ) getOrElse Future(BadRequest)
  }

  def addUsername() = Action.async(parse.tolerantJson) { implicit request: Request[JsValue] =>
    ((request.body \ "name").asOpt[String], parseToken(request))
      .mapN((name, token) => postService.addUsername(token, name).map(_ => Ok)) getOrElse Future(BadRequest)
  }

  def parseToken(req: Request[_]) =
    req.headers
      .get("Auth-Token")
      .flatMap(_.toLongOption)

}
