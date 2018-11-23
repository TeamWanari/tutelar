package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.ProviderApi
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.providers.oauth2.OAuth2Api.CodeAndState

import scala.concurrent.Future
import scala.util.Success

trait OAuth2Api extends ProviderApi {
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
              completeLoginFlow(service.authenticateWithCallback(codeAndState.code, codeAndState.state))
            }
          }
      }
    }
  }
}

object OAuth2Api {
  class FacebookApi(implicit val service: FacebookService[Future], val callbackConfig: () => Future[CallbackConfig])
      extends OAuth2Api {}
  class GithubApi(implicit val service: GithubService[Future], val callbackConfig: () => Future[CallbackConfig])
      extends OAuth2Api {}
  class GoogleApi(implicit val service: GoogleService[Future], val callbackConfig: () => Future[CallbackConfig])
      extends OAuth2Api {}

  case class CodeAndState(code: String, state: String)
}
