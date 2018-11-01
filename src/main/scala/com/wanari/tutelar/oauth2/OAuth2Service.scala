package com.wanari.tutelar.oauth2

import akka.http.scaladsl.model._
import cats.{Applicative, MonadError}
import com.wanari.tutelar.{AuthService, CsrfService}
import com.wanari.tutelar.oauth2.OAuth2Service.{IdAndRaw, TokenRequestHelper, TokenResponseHelper}
import com.wanari.tutelar.util.HttpWrapper
import spray.json.{JsObject, JsString, JsValue, RootJsonFormat, RootJsonReader}

trait OAuth2Service[F[_]] {
  val config: OAuth2ConfigService[F]
  val csrfService: CsrfService[F]
  val http: HttpWrapper[F]
  val authService: AuthService[F]

  val TYPE: String
  val redirectUriBase: Uri

  protected def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri): HttpRequest
  protected def getApi(token: TokenResponseHelper): F[IdAndRaw]

  def generateIdentifierUrl(implicit me: MonadError[F, Throwable]): F[Uri] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    for {
      state           <- csrfService.getCsrfToken(TYPE)
      clientId        <- config.getClientId
      scopes          <- config.getScopes
      selfRedirectUri <- getSelfRedirectUri
    } yield
      redirectUriBase.withQuery(
        Uri.Query(
          "client_id"     -> clientId,
          "scope"         -> scopes.mkString(" "),
          "state"         -> state,
          "response_type" -> "code",
          "redirect_uri"  -> selfRedirectUri.toString
        )
      )
  }

  def authenticateWithCallback(code: String, state: String)(implicit me: MonadError[F, Throwable]): F[String] = {
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
      clientId        <- config.getClientId
      clientSecret    <- config.getClientSecret
      selfRedirectUri <- getSelfRedirectUri
      response        <- getToken(clientId, clientSecret, selfRedirectUri)
      tokenResponse   <- http.unmarshalEntityTo[TokenResponseHelper](response)
      idAndRaw        <- getApi(tokenResponse)
      url             <- authService.registerOrLogin(TYPE, idAndRaw.id, tokenResponse.access_token)
    } yield url
  }

  protected def getSelfRedirectUri(implicit applicative: Applicative[F]): F[Uri] = {
    import cats.syntax.functor._
    config.getRootUrl.map { selfRedirectUriBase =>
      val uri = Uri(selfRedirectUriBase)
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

  case class IdAndRaw(id: String, raw: JsValue)

  import spray.json.DefaultJsonProtocol._

  implicit val tokenRequestHelperFormat: RootJsonFormat[TokenRequestHelper]   = jsonFormat4(TokenRequestHelper)
  implicit val tokenResponseHelperReader: RootJsonReader[TokenResponseHelper] = jsonFormat1(TokenResponseHelper)
  def idAndRawReader(idKey: String): RootJsonReader[IdAndRaw] = new RootJsonReader[IdAndRaw] {
    //TODO: error handling
    def read(value: JsValue): IdAndRaw = value match {
      case obj: JsObject =>
        obj.fields.get(idKey).fold(throw new Exception()) {
          case strId: JsString =>
            IdAndRaw(strId.value, obj)
          case idVal: JsValue =>
            IdAndRaw(idVal.compactPrint, obj)
        }
      case _ =>
        throw new Exception()
    }
  }
}
