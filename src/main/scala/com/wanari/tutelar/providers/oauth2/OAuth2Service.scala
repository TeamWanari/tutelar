package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import cats.{Applicative, MonadError}
import com.wanari.tutelar.core.AuthService.Token
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
  val oAuth2config: () => F[OAuth2Config]
  val csrfService: CsrfService[F]
  val http: HttpWrapper[F]
  val authService: AuthService[F]

  val TYPE: String
  val redirectUriBase: Uri

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest
  protected def getProfile(token: TokenResponseHelper)(implicit ctx: LogContext): F[ProfileData]

  def generateIdentifierUrl(implicit me: MonadError[F, Throwable]): F[Uri] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    for {
      config          <- oAuth2config()
      state           <- csrfService.getCsrfToken(TYPE, JsObject.empty)
      selfRedirectUri <- getSelfRedirectUri
    } yield {
      redirectUriBase.withQuery(
        Uri.Query(
          "client_id"     -> config.clientId,
          "scope"         -> config.scopes.mkString(" "),
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
  )(implicit me: MonadError[F, Throwable], ctx: LogContext): F[Token] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._

    def getToken(clientId: String, clientSecret: String, selfRedirectUri: Uri) = {
      http.singleRequest(
        createTokenRequest(
          TokenRequestHelper(clientId, clientSecret, code, state),
          selfRedirectUri
        )
      )
    }

    for {
      _               <- csrfService.checkCsrfToken(TYPE, state)
      config          <- oAuth2config()
      selfRedirectUri <- getSelfRedirectUri
      response        <- getToken(config.clientId, config.clientSecret, selfRedirectUri)
      tokenResponse   <- http.unmarshalEntityTo[TokenResponseHelper](response)
      profile         <- getProfile(tokenResponse)
      token           <- authService.registerOrLogin(TYPE, profile.id, tokenResponse.access_token, profile.data)
    } yield token
  }

  def authenticateWithAccessToken(
      accessToken: String
  )(implicit me: MonadError[F, Throwable], ctx: LogContext): F[Token] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._

    for {
      profile <- getProfile(TokenResponseHelper(accessToken))
      token   <- authService.registerOrLogin(TYPE, profile.id, accessToken, profile.data)
    } yield token
  }

  protected def getSelfRedirectUri(implicit applicative: Applicative[F]): F[Uri] = {
    import cats.syntax.functor._
    oAuth2config().map { config =>
      val uri = Uri(config.rootUrl)
      uri.withPath(uri.path ?/ TYPE.toLowerCase / "callback")
    }
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
  def profileDataReader(idKey: String): RootJsonReader[ProfileData] = new RootJsonReader[ProfileData] {
    //TODO: error handling
    def read(value: JsValue): ProfileData = value match {
      case obj: JsObject =>
        obj.fields.get(idKey).fold(throw new Exception()) {
          case strId: JsString =>
            ProfileData(strId.value, obj)
          case idVal: JsValue =>
            ProfileData(idVal.compactPrint, obj)
        }
      case _ =>
        throw new Exception()
    }
  }

  case class OAuth2Config(
      rootUrl: String,
      clientId: String,
      clientSecret: String,
      scopes: Seq[String]
  )
}
