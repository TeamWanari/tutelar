package com.wanari.tutelar.core
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete, redirect}
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.core.ProviderApi._
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ProviderApi extends Api {
  val callbackConfig: CallbackConfig

  def completeLoginFlowWithRedirect(token: ErrorOr[Future, TokenData]): Route = {
    onComplete(token.value) {
      case Success(Right(t)) => redirect(generateCallbackUrl(t), StatusCodes.Found)
      case Success(Left(_)) =>
        redirect(generateErrorCallbackUrl(ProviderApi.Errors.AUTHENTICATION_FAILED), StatusCodes.Found)
      case Failure(_) => redirect(generateErrorCallbackUrl(ProviderApi.Errors.AUTHENTICATION_FAILED), StatusCodes.Found)
    }
  }

  def completeLoginFlowWithJson(token: ErrorOr[Future, TokenData]): Route = {
    onComplete(token.value) {
      case Success(Right(t)) => complete(t)
      case Success(Left(_))  => complete((StatusCodes.Unauthorized, ErrorData(ProviderApi.Errors.AUTHENTICATION_FAILED)))
      case Failure(_)        => complete((StatusCodes.Unauthorized, ErrorData(ProviderApi.Errors.AUTHENTICATION_FAILED)))
    }
  }

  private def generateCallbackUrl(tokenData: TokenData): String = {
    callbackConfig.success
      .replace("<<TOKEN>>", tokenData.token)
      .replace("<<REFRESH_TOKEN>>", tokenData.refreshToken)
  }

  private def generateErrorCallbackUrl(error: AuthError): String = {
    callbackConfig.failure.replace("<<ERROR>>", error)
  }
}

object ProviderApi {
  case class CallbackConfig(success: String, failure: String)

  case class LoginData(username: String, password: String, data: Option[JsObject], refreshToken: Option[LongTermToken])
  case class ErrorData(error: AuthError)

  import DefaultJsonProtocol._
  implicit val loginDataFormat: RootJsonFormat[LoginData] = jsonFormat4(LoginData)
  implicit val tokenDataFormat: RootJsonFormat[TokenData] = jsonFormat2(TokenData)
  implicit val errorDataFormat: RootJsonFormat[ErrorData] = jsonFormat1(ErrorData)

  type AuthError = String
  object Errors {
    val AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
  }
}
