package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model._
import cats.Monad
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.RootJsonReader

class MicrosoftService[F[_]: Monad](val oAuth2config: OAuth2Config)(implicit
    val authService: AuthService[F],
    val csrfService: CsrfService[F],
    val http: HttpWrapper[F]
) extends OAuth2Service[F] {
  import OAuth2Service._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  val TYPE            = "microsoft"
  val redirectUriBase = Uri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
  val tokenEndpoint   = Uri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
  val userEndpoint    = Uri("https://graph.microsoft.com/v1.0/me")

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest = {
    HttpRequest(
      HttpMethods.POST,
      tokenEndpoint,
      Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil,
      entityHelper.formEntity(selfRedirectUri.toString)
    )
  }

  protected def getProfile(token: TokenResponseHelper)(implicit ctx: LogContext): F[ProfileData] = {
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
