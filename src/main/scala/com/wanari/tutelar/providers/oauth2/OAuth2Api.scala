package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.emarsys.escher.akka.http.config.EscherConfig
import com.wanari.tutelar.core.AuthService.LongTermToken
import com.wanari.tutelar.core.Errors.{AppError, ErrorHandler, ErrorResponse}
import com.wanari.tutelar.core.ProviderApi.CallbackConfig
import com.wanari.tutelar.core.ServiceAuthDirectives.ServiceAuthConfig
import com.wanari.tutelar.core.{ProviderApi, ServiceAuthDirectives}
import com.wanari.tutelar.providers.oauth2.OAuth2Api.{AccessToken, CodeAndState}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

sealed abstract class OAuth2Api(implicit
    val getServiceAuthConfig: String => ServiceAuthConfig,
    val escherConfig: EscherConfig
) extends ProviderApi
    with ServiceAuthDirectives {
  import cats.instances.future._
  val service: OAuth2Service[Future]

  override protected def authConfig: ServiceAuthConfig = getServiceAuthConfig("oauth2.oauth2Api")

  def route(): Route = {
    pathPrefix(service.TYPE.toLowerCase) {
      extractExecutionContext { implicit executor =>
        path("login") {
          post {
            entity(as[AccessToken]) { data =>
              withTrace(s"Login_${service.TYPE.toLowerCase}") { implicit ctx =>
                completeLoginFlowWithJson(service.authenticateWithAccessToken(data.accessToken, data.refreshToken))
              }
            }
          } ~
            parameter("refresh_token".?) { refreshToken =>
              onComplete(service.generateIdentifierUrl(refreshToken)) {
                case Success(value) => redirect(value, StatusCodes.Found)
                case _              => complete(StatusCodes.Unauthorized)
              }
            }
        } ~
          path("callback") {
            parameters((Symbol("code").as[String], Symbol("state").as[String])).as(CodeAndState) { codeAndState =>
              withTrace(s"Callback_${service.TYPE.toLowerCase}") { implicit ctx =>
                completeLoginFlowWithRedirect(
                  service.authenticateWithCallback(codeAndState.code, codeAndState.state)
                )
              }
            }
          } ~
          path("token") {
            authenticateService {
              parameters(Symbol("userId").as[String]) { userId =>
                withTrace(s"Token_${service.TYPE.toLowerCase}") { implicit ctx =>
                  val customHandler: ErrorHandler = {
                    case appError: AppError =>
                      logger.info(appError.message)
                      complete((StatusCodes.NotFound, ErrorResponse(appError.message)))
                  }

                  service.getAccessTokenForUser(userId).toComplete(customHandler)
                }
              }
            }
          }
      }
    }
  }
}

object OAuth2Api {
  class FacebookApi(implicit
      val service: FacebookService[Future],
      val callbackConfig: CallbackConfig,
      val ec: ExecutionContext,
      override val getServiceAuthConfig: String => ServiceAuthConfig,
      override val escherConfig: EscherConfig
  ) extends OAuth2Api {}
  class GithubApi(implicit
      val service: GithubService[Future],
      val callbackConfig: CallbackConfig,
      val ec: ExecutionContext,
      override val getServiceAuthConfig: String => ServiceAuthConfig,
      override val escherConfig: EscherConfig
  ) extends OAuth2Api {}
  class GoogleApi(implicit
      val service: GoogleService[Future],
      val callbackConfig: CallbackConfig,
      val ec: ExecutionContext,
      override val getServiceAuthConfig: String => ServiceAuthConfig,
      override val escherConfig: EscherConfig
  ) extends OAuth2Api {}
  class MicrosoftApi(implicit
      val service: MicrosoftService[Future],
      val callbackConfig: CallbackConfig,
      val ec: ExecutionContext,
      override val getServiceAuthConfig: String => ServiceAuthConfig,
      override val escherConfig: EscherConfig
  ) extends OAuth2Api {}

  case class CodeAndState(code: String, state: String)
  case class AccessToken(accessToken: String, refreshToken: Option[LongTermToken])

  import DefaultJsonProtocol._
  implicit val accessTokenFormat: RootJsonFormat[AccessToken] = jsonFormat2(AccessToken)
}
