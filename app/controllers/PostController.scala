package controllers

import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import services.PostService
import utils.JsonUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostController @Inject()(postService: PostService, cc: ControllerComponents)(implicit val ec: ExecutionContext) extends AbstractController(cc) {


  def index() = Action.async { implicit request =>
    postService.getPosts().map(c =>
      Ok(Json.toJson(c.toString)))
  }
  def addUserToGroup(id: Long) = Action.async { implicit request =>
    postService.addUserToGroup(2, id).map(c =>
      Ok(Json.toJson(c.toString)))
  }

  def getUserGroups(id: Long) = Action.async { implicit request =>
  postService.getUserGroups(id).map(c =>
    Ok(Json.toJson(c.toString)))
  }


}
