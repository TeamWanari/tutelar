package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import cats.MonadError
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.util.HttpWrapper
import spray.json.RootJsonReader

// https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/
class GithubService[F[_]: MonadError[?[_], Throwable]](val oAuth2config: () => F[OAuth2Config])(
    implicit
    val authService: AuthService[F],
    val csrfService: CsrfService[F],
    val http: HttpWrapper[F]
) extends OAuth2Service[F] {
  import OAuth2Service._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  val TYPE            = "github"
  val redirectUriBase = Uri("https://github.com/login/oauth/authorize")
  val tokenEndpoint   = Uri("https://github.com/login/oauth/access_token")
  val userEndpoint    = Uri("https://api.github.com/user")

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest = {
    HttpRequest(
      HttpMethods.POST,
      tokenEndpoint,
      Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil,
      entityHelper.jsonEntity
    )
  }

  protected def getProfile(token: TokenResponseHelper): F[ProfileData] = {
    implicit val idAndRawR: RootJsonReader[ProfileData] = profileDataReader("id")

    val request =
      HttpRequest(
        HttpMethods.GET,
        userEndpoint,
        Authorization(OAuth2BearerToken(token.access_token)) :: Accept(
          MediaRange.One(MediaTypes.`application/json`, 1.0f)
        ) :: Nil
      )

    for {
      resp <- http.singleRequest(request)
      ret  <- http.unmarshalEntityTo[ProfileData](resp)
    } yield ret
  }

}
