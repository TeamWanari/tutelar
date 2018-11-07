package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.providers.oauth2.OAuth2Api.CodeAndState

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait OAuth2Api extends Api {
  import cats.instances.future._
  val service: OAuth2Service[Future]

  def route(): Route = {
    pathPrefix(service.TYPE.toLowerCase) {
      extractExecutionContext { implicit executor =>
        path("login") {
          onComplete(service.generateIdentifierUrl) {
            case Success(value) => redirect(value, StatusCodes.Found)
            case _              => complete(StatusCodes.Unauthorized)
          }
        } ~
          path("callback") {
            parameters(('code.as[String], 'state.as[String])).as(CodeAndState) { codeAndState =>
              onComplete(service.authenticateWithCallback(codeAndState.code, codeAndState.state)) {
                case Success(value) => redirect(value, StatusCodes.Found)
                case Failure(ex)    => complete(StatusCodes.Unauthorized)
              }
            }
          }
      }
    }
  }
}

object OAuth2Api {
  class FacebookApi(implicit val service: FacebookService[Future]) extends OAuth2Api {}
  class GithubApi(implicit val service: GithubService[Future])     extends OAuth2Api {}
  class GoogleApi(implicit val service: GoogleService[Future])     extends OAuth2Api {}

  case class CodeAndState(code: String, state: String)
}
