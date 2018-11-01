package com.wanari.tutelar.oauth2

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import cats.MonadError
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.{AuthService, CsrfService}

class GoogleService[F[_]: MonadError[?[_], Throwable]](val config: OAuth2ConfigService[F])(
    implicit
    val authService: AuthService[F],
    val csrfService: CsrfService[F],
    val http: HttpWrapper[F]
) extends OAuth2Service[F] {
  import OAuth2Service._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  val TYPE            = "google"
  val redirectUriBase = Uri("https://accounts.google.com/o/oauth2/v2/auth")
  val tokenEndpoint   = Uri("https://www.googleapis.com/oauth2/v4/token")
  val userEndpoint    = Uri("https://www.googleapis.com/oauth2/v3/userinfo")

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest = {
    HttpRequest(
      HttpMethods.POST,
      tokenEndpoint,
      Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil,
      entityHelper.formEntity(selfRedirectUri.toString)
    )
  }

  protected def getApi(token: TokenResponseHelper): F[IdAndRaw] = {
    implicit val idAndRawR = idAndRawReader("sub")

    val request =
      HttpRequest(
        HttpMethods.GET,
        userEndpoint,
        Authorization(OAuth2BearerToken(token.access_token)) ::
          Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) ::
          Nil
      )

    for {
      resp <- http.singleRequest(request)
      ret  <- http.unmarshalEntityTo[IdAndRaw](resp)
    } yield ret
  }
}
