package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, OAuth2BearerToken}
import cats.Monad
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.OAuth2Config
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.RootJsonReader

class FacebookService[F[_]: Monad](
    val oAuth2config: OAuth2Config
)(implicit
    val authService: AuthService[F],
    val csrfService: CsrfService[F],
    val http: HttpWrapper[F]
) extends OAuth2Service[F] {
  import OAuth2Service._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  val TYPE            = "facebook"
  val redirectUriBase = Uri("https://www.facebook.com/v4.0/dialog/oauth")
  val tokenEndpoint   = Uri("https://graph.facebook.com/v4.0/oauth/access_token")
  val userEndpoint = Uri(
    "https://graph.facebook.com/v4.0/me?fields=id,first_name,last_name,middle_name,name,name_format,picture,short_name"
  )

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest = {
    val endpoint = tokenEndpoint.withQuery(
      Uri.Query(entityHelper.getAsMap(selfRedirectUri.toString))
    )
    HttpRequest(
      HttpMethods.GET,
      endpoint,
      Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil
    )
  }

  protected def getProfile(token: TokenResponseHelper)(implicit ctx: LogContext): F[ProfileData] = {
    implicit val idAndRawR: RootJsonReader[ProfileData] = profileDataReader("id")

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
