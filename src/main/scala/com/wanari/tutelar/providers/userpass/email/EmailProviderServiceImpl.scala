package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.core.{AuthService, JwtService}
import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.providers.userpass.basic.BasicProviderServiceImpl
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import com.wanari.tutelar.util.PasswordCryptor
import spray.json.{JsObject, JsString}

class EmailProviderServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit emailService: EmailService[F],
    configF: () => F[EmailProviderConfig],
    authService: AuthService[F],
    jwtService: F[JwtService[F]]
) extends BasicProviderServiceImpl
    with EmailProviderService[F]
    with PasswordCryptor {

  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  override protected val authType = "EMAIL"

  override def register(registerToken: String, password: String, data: Option[JsObject]): F[Token] = {
    for {
      email <- decodeToken(registerToken)
      token <- super.register(email, password, data)
    } yield token
  }

  def sendRegister(email: String): F[Unit] = {
    for {
      token  <- createRegistrationToken(email)
      url    <- createRegistrationUrl(token)
      result <- emailService.sendRegisterUrl(email, url)
    } yield result
  }

  private def decodeToken(registerToken: String): F[String] = {
    (for {
      service   <- jwtService
      tokenData <- service.decode(registerToken)
    } yield {
      tokenData.fields
        .get("email")
        .collect {
          case JsString(email) => email
        }
    }).flatMap(_.pureOrRaise(new Exception()))
  }

  private def createRegistrationToken(email: String): F[String] = {
    for {
      service <- jwtService
      token   <- service.encode(JsObject("email" -> JsString(email)))
    } yield token
  }

  private def createRegistrationUrl(registerToken: String): F[Token] = {
    configF().map(_.registerUrl.replace("<<TOKEN>>", registerToken))
  }
}
