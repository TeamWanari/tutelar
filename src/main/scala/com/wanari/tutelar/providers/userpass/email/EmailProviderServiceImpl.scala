package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.providers.userpass.PasswordDifficultyChecker
import com.wanari.tutelar.providers.userpass.basic.BasicProviderServiceImpl
import com.wanari.tutelar.providers.userpass.email.EmailProviderServiceImpl.EmailToken
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json._

class EmailProviderServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit emailService: EmailService[F],
    authService: AuthService[F],
    passwordDifficultyChecker: PasswordDifficultyChecker[F],
    jwtService: JwtService[F]
) extends BasicProviderServiceImpl
    with EmailProviderService[F] {

  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  override protected val authType = "EMAIL"

  override def register(registerToken: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[Token] = {
    for {
      email <- decodeToken(registerToken, EmailToken.RegisterType)
      token <- super.register(email, password, data)
    } yield token
  }

  def sendRegister(email: String)(implicit ctx: LogContext): F[Unit] = {
    for {
      token  <- createToken(email, EmailToken.RegisterType)
      result <- emailService.sendRegisterUrl(email, token)
    } yield result
  }

  override def resetPassword(resetPasswordToken: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[Token] = {
    for {
      email <- decodeToken(resetPasswordToken, EmailToken.ResetPasswordType)
      token <- changePasswordAndLogin(email, password, data)
    } yield token
  }

  def sendResetPassword(email: String)(implicit ctx: LogContext): F[Unit] = {
    for {
      _      <- checkIsExists(email)
      token  <- createToken(email, EmailToken.ResetPasswordType)
      result <- emailService.sendResetPasswordUrl(email, token)
    } yield result
  }

  private def decodeToken(registerToken: String, `type`: String): F[String] = {
    import EmailProviderServiceImpl._
    for {
      tokenData  <- jwtService.validateAndDecode(registerToken)
      emailToken <- tokenData.convertToF[F, EmailToken]
      _          <- (emailToken.`type` == `type`).pureUnitOrRise(new Exception)
    } yield emailToken.email
  }

  private def createToken(email: String, `type`: String): F[String] = {
    jwtService.encode(EmailToken(email, `type`).toJson.asJsObject)
  }

  private def changePasswordAndLogin(email: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[Token] = {
    for {
      _     <- checkIsExists(email)
      token <- authService.registerOrLogin(authType, email, encryptPassword(password), data.getOrElse(JsObject()))
    } yield token
  }

  private def checkIsExists(email: String): F[Unit] = {
    authService.findCustomData(authType, email).pureOrRaise(new Exception).map(_ => ())
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
