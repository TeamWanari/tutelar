package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import cats.MonadError
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config

class GoogleService[F[_]: MonadError[?[_], Throwable]](val oAuth2config: () => F[OAuth2Config])(
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

  protected def getProfile(token: TokenResponseHelper): F[ProfileData] = {
    implicit val idAndRawR = profileDataReader("sub")

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
      ret  <- http.unmarshalEntityTo[ProfileData](resp)
    } yield ret
  }
}
