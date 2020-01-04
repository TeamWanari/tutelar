package com.wanari.tutelar.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import cats.data.EitherT
import com.wanari.tutelar.util.LoggerUtil.{LogContext, Logger}
import spray.json.{JsObject, RootJsonFormat, RootJsonWriter}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Errors {
  type ErrorOr[F[_], T] = EitherT[F, AppError, T]

  sealed trait AppError {
    def message: String
  }
  sealed abstract class ConfigError(val msg: String) extends Throwable
  case class WrongConfig(override val msg: String)   extends ConfigError(msg)

  sealed abstract class AuthError(val message: String) extends AppError
  case class UserNotFound()                            extends AuthError("User not found")
  case class AccountNotFound()                         extends AuthError("Account not found")
  case class AccountUsed()                             extends AuthError("Account already used")
  case class UserHadThisAccountType()                  extends AuthError("User already had this type of account")
  case class UserLastAccount()                         extends AuthError("This account is the users last account")
  case class AuthenticationFailed()                    extends AuthError("Authentication failed, wrong credentials")
  case class InvalidToken()                            extends AuthError("Invalid token")
  case class LoginExpired()                            extends AuthError("Login expired")
  case class AccountBelongsToOtherUser()               extends AuthError("Account belongs to other user!")

  sealed abstract class Oauth2Error(val message: String) extends Throwable with AppError
  case class InvalidCsrfToken()                          extends Oauth2Error("Wrong csrf token")
  case class InvalidProfileDataMissingKey(key: String)   extends Oauth2Error(s"Invalid profile data, missing key: $key")
  case class InvalidProfileDataNotJsonObject()           extends Oauth2Error("Invalid profile data, is not a JSON object")

  sealed abstract class JwtError(val message: String) extends AppError
  case class InvalidJwt()                             extends JwtError("Invalid JWT")

  sealed abstract class UserPassError(val message: String) extends AppError
  case class WeakPassword()                                extends UserPassError("The password is weak")
  case class UsernameUsed()                                extends UserPassError("Username already used")

  sealed abstract class EmailPassError(msg: String) extends UserPassError(msg)
  case class InvalidEmailToken()                    extends EmailPassError("Invalid email token")

  sealed abstract class EmailHttpServiceError(val message: String) extends Throwable with AppError
  case class HttpClientError(response: HttpResponse)
      extends EmailHttpServiceError(s"Error: StatusCode=${response.status}")

  sealed abstract class TotpError(val message: String) extends AppError
  case class InvalidAlgo(algo: String)                 extends TotpError(s"Invalid algo $algo")
  case class WrongPassword()                           extends TotpError("Wrong password")

  implicit class ResponseWrapper[T](val response: ErrorOr[Future, T]) {
    def toComplete(implicit w: RootJsonWriter[T], ctx: LogContext, logger: Logger): Route = {
      toComplete(None)
    }
    def toComplete(handler: ErrorHandler)(implicit w: RootJsonWriter[T], ctx: LogContext, logger: Logger): Route = {
      toComplete(Option(handler))
    }

    private def toComplete(
        mbHandler: Option[ErrorHandler]
    )(implicit w: RootJsonWriter[T], ctx: LogContext, logger: Logger) = {
      val defaultHandler: ErrorHandler = {
        case appError: AppError =>
          logger.info(appError.message)
          complete((StatusCodes.Unauthorized, ErrorResponse(appError.message)))
      }
      val errorHandler = mbHandler.map(_.orElse(defaultHandler)).getOrElse(defaultHandler)

      onComplete(response.value) {
        case Success(Right(res))  => complete(res)
        case Success(Left(error)) => errorHandler(error)
        case Failure(error) =>
          logger.error("Unhandled error!", error)
          complete(StatusCodes.InternalServerError)
      }
    }
  }

  type ErrorHandler = PartialFunction[AppError, Route]

  case class ErrorResponse(error: String)
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
  implicit val unitWriter: RootJsonWriter[Unit]                   = (_: Unit) => JsObject()
}
