package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.providers.userpass.basic.BasicProviderServiceImpl
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import com.wanari.tutelar.providers.userpass.email.EmailProviderServiceImpl.EmailToken
import spray.json._

class EmailProviderServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit emailService: EmailService[F],
    configF: () => F[EmailProviderConfig],
    authService: AuthService[F],
    jwtService: F[JwtService[F]]
) extends BasicProviderServiceImpl
    with EmailProviderService[F] {

  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  override protected val authType = "EMAIL"

  override def register(registerToken: String, password: String, data: Option[JsObject]): F[Token] = {
    for {
      email <- decodeToken(registerToken, EmailToken.RegisterType)
      token <- super.register(email, password, data)
    } yield token
  }

  def sendRegister(email: String): F[Unit] = {
    for {
      token  <- createToken(email, EmailToken.RegisterType)
      url    <- createRegistrationUrl(token)
      result <- emailService.sendRegisterUrl(email, url)
    } yield result
  }

  override def resetPassword(resetPasswordToken: String, password: String, data: Option[JsObject]): F[Token] = {
    for {
      email <- decodeToken(resetPasswordToken, EmailToken.ResetPasswordType)
      token <- changePasswordAndLogin(email, password, data)
    } yield token
  }

  def sendResetPassword(email: String): F[Unit] = {
    for {
      _      <- checkIsExists(email)
      token  <- createToken(email, EmailToken.ResetPasswordType)
      url    <- createResetPasswordUrl(token)
      result <- emailService.sendResetPasswordUrl(email, url)
    } yield result
  }

  private def decodeToken(registerToken: String, `type`: String): F[String] = {
    import EmailProviderServiceImpl._
    for {
      service    <- jwtService
      tokenData  <- service.validateAndDecode(registerToken)
      emailToken <- tokenData.convertToF[F, EmailToken]
      _          <- (emailToken.`type` == `type`).pureUnitOrRise(new Exception)
    } yield emailToken.email
  }

  private def createToken(email: String, `type`: String): F[String] = {
    for {
      service <- jwtService
      token   <- service.encode(EmailToken(email, `type`).toJson.asJsObject)
    } yield token
  }

  private def createRegistrationUrl(registerToken: String): F[Token] = {
    configF().map(_.registerUrl.replace("<<TOKEN>>", registerToken))
  }

  private def createResetPasswordUrl(resetPasswordToken: String): F[Token] = {
    configF().map(_.resetPasswordUrl.replace("<<TOKEN>>", resetPasswordToken))
  }

  private def changePasswordAndLogin(email: String, password: String, data: Option[JsObject]): F[Token] = {
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
