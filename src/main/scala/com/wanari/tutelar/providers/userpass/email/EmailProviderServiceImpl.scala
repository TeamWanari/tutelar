package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.{AppError, ErrorOr, InvalidEmailToken, UserNotFound}
import com.wanari.tutelar.core.impl.JwtServiceImpl
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.providers.userpass.PasswordDifficultyChecker
import com.wanari.tutelar.providers.userpass.basic.BasicProviderServiceImpl
import com.wanari.tutelar.providers.userpass.email.EmailProviderServiceImpl.EmailToken
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json._

import scala.util.{Success, Try}

class EmailProviderServiceImpl[F[_]: MonadError[*[_], Throwable]](implicit
    emailService: EmailService[F],
    authService: AuthService[F],
    passwordDifficultyChecker: PasswordDifficultyChecker[F],
    getJwtConfig: String => JwtConfig
) extends BasicProviderServiceImpl
    with EmailProviderService[F] {
  override protected val authType = "EMAIL"

  protected val jwtService: JwtService[F] = new JwtServiceImpl[F](getJwtConfig("emailProvider"))

  override def init: F[Unit] = {
    jwtService.init
  }

  override def register(
      registerToken: String,
      password: String,
      data: Option[JsObject],
      refreshToken: Option[LongTermToken]
  )(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      email <- decodeToken(registerToken, EmailToken.RegisterType)
      token <- super.register(email, password, data, refreshToken)
    } yield token
  }

  def sendRegister(email: String)(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    for {
      token  <- EitherT.right(createToken(email, EmailToken.RegisterType))
      result <- EitherT.right(emailService.sendRegisterUrl(email, token))
    } yield result
  }

  override def resetPassword(resetPasswordToken: String, password: String, data: Option[JsObject])(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      email <- decodeToken(resetPasswordToken, EmailToken.ResetPasswordType)
      token <- changePasswordAndLogin(email, password, data)
    } yield token
  }

  def sendResetPassword(email: String)(implicit ctx: LogContext): ErrorOr[F, Unit] = {
    for {
      _      <- checkIsExists(email)
      token  <- EitherT.right(createToken(email, EmailToken.ResetPasswordType))
      result <- EitherT.right(emailService.sendResetPasswordUrl(email, token))
    } yield result
  }

  private def decodeToken(registerToken: String, `type`: String): ErrorOr[F, String] = {
    import EmailProviderServiceImpl._
    def decodeToEmailToken(tokenData: JsObject): Either[AppError, EmailToken] = {
      Try(tokenData.convertTo[EmailToken]).filter(_.`type` == `type`) match {
        case Success(value) => Right(value)
        case _              => Left(InvalidEmailToken())
      }
    }
    for {
      tokenData  <- jwtService.validateAndDecode(registerToken)
      emailToken <- EitherT.fromEither(decodeToEmailToken(tokenData))
    } yield emailToken.email
  }

  private def createToken(email: String, `type`: String): F[String] = {
    jwtService.encode(EmailToken(email, `type`).toJson.asJsObject)
  }

  private def changePasswordAndLogin(email: String, password: String, data: Option[JsObject])(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData] = {
    for {
      _ <- checkIsExists(email)
      token <- authService.authenticatedWith(
        authType,
        email,
        encryptPassword(password),
        data.getOrElse(JsObject()),
        None
      )
    } yield token
  }

  private def checkIsExists(email: String): ErrorOr[F, Unit] = {
    authService.findCustomData(authType, email).toRight[AppError](UserNotFound()).map(_ => ())
  }
}

object EmailProviderServiceImpl {
  case class EmailToken(email: String, `type`: String)
  object EmailToken {
    val RegisterType      = "register"
    val ResetPasswordType = "reset"
  }

  import DefaultJsonProtocol._
  implicit val emailTokenFormat: RootJsonFormat[EmailToken] = jsonFormat2(EmailToken.apply)
}
