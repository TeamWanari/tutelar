package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import cats.data.EitherT
import cats.MonadError
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.{ErrorOr, InvalidProfileDataMissingKey, InvalidProfileDataNotJsonObject}
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.{
  OAuth2Config,
  ProfileData,
  TokenRequestHelper,
  TokenResponseHelper
}
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat, RootJsonReader}

trait OAuth2Service[F[_]] {
  def oAuth2config: OAuth2Config
  def csrfService: CsrfService[F]
  def http: HttpWrapper[F]
  def authService: AuthService[F]

  def TYPE: String
  def redirectUriBase: Uri

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest
  protected def getProfile(token: TokenResponseHelper)(implicit ctx: LogContext): F[ProfileData]

  def generateIdentifierUrl(implicit me: MonadError[F, Throwable]): F[Uri] = {
    import cats.syntax.functor._
    for {
      state <- csrfService.getCsrfToken(TYPE, JsObject.empty)
    } yield {
      redirectUriBase.withQuery(
        Uri.Query(
          "client_id"     -> oAuth2config.clientId,
          "scope"         -> oAuth2config.scopes.mkString(" "),
          "state"         -> state,
          "response_type" -> "code",
          "redirect_uri"  -> selfRedirectUri.toString
        )
      )
    }
  }

  def authenticateWithCallback(
      code: String,
      state: String
  )(implicit me: MonadError[F, Throwable], ctx: LogContext): ErrorOr[F, TokenData] = {
    def getToken(clientId: String, clientSecret: String, selfRedirectUri: Uri) = {
      http.singleRequest(
        createTokenRequest(
          TokenRequestHelper(clientId, clientSecret, code, state),
          selfRedirectUri
        )
      )
    }

    for {
      _             <- csrfService.checkCsrfToken(TYPE, state)
      response      <- EitherT.right(getToken(oAuth2config.clientId, oAuth2config.clientSecret, selfRedirectUri))
      tokenResponse <- EitherT.right(http.unmarshalEntityTo[TokenResponseHelper](response))
      profile       <- EitherT.right(getProfile(tokenResponse))
      token         <- authService.registerOrLogin(TYPE, profile.id, tokenResponse.access_token, profile.data)
    } yield token
  }

  def authenticateWithAccessToken(
      accessToken: String
  )(implicit me: MonadError[F, Throwable], ctx: LogContext): ErrorOr[F, TokenData] = {
    for {
      profile <- EitherT.right(getProfile(TokenResponseHelper(accessToken)))
      token   <- authService.registerOrLogin(TYPE, profile.id, accessToken, profile.data)
    } yield token
  }

  protected lazy val selfRedirectUri: Uri = {
    val uri = Uri(oAuth2config.rootUrl)
    uri.withPath(uri.path ?/ TYPE.toLowerCase / "callback")
  }
}

object OAuth2Service {

  case class TokenRequestHelper(client_id: String, client_secret: String, code: String, state: String) {
    def jsonEntity: RequestEntity =
      HttpEntity(ContentTypes.`application/json`, tokenRequestHelperFormat.write(this).compactPrint)

    def getAsMap(redirectUri: String): Map[String, String] = Map(
      "client_id"     -> client_id,
      "client_secret" -> client_secret,
      "code"          -> code,
      "state"         -> state,
      "grant_type"    -> "authorization_code",
      "redirect_uri"  -> redirectUri
    )

    def formEntity(redirectUri: String): RequestEntity =
      FormData(
        getAsMap(redirectUri)
      ).toEntity
  }

  case class TokenResponseHelper(access_token: String)

  case class ProfileData(id: String, data: JsObject)

  import spray.json.DefaultJsonProtocol._

  implicit val tokenRequestHelperFormat: RootJsonFormat[TokenRequestHelper]   = jsonFormat4(TokenRequestHelper)
  implicit val tokenResponseHelperReader: RootJsonReader[TokenResponseHelper] = jsonFormat1(TokenResponseHelper)
  def profileDataReader(idKey: String): RootJsonReader[ProfileData] = {
    case obj: JsObject =>
      obj.fields.get(idKey).fold(throw InvalidProfileDataMissingKey(idKey)) {
        case strId: JsString =>
          ProfileData(strId.value, obj)
        case idVal: JsValue =>
          ProfileData(idVal.compactPrint, obj)
      }
    case _ =>
      throw InvalidProfileDataNotJsonObject()
  }

  case class OAuth2Config(
      rootUrl: String,
      clientId: String,
      clientSecret: String,
      scopes: Seq[String]
  )
}
