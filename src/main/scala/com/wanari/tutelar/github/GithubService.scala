package com.wanari.tutelar.github

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, RawHeader}
import cats.MonadError
import com.wanari.tutelar.CsrfService
import com.wanari.tutelar.util.HttpWrapper
import spray.json.{JsObject, JsValue, RootJsonFormat, RootJsonReader}

//TODO: errorhandling
trait GithubService[F[_]] {
  val TYPE = "GITHUB"
  def authenticateWithCallback(params: Map[String, String]): F[Unit]
  def generateIdentifierUrl: F[Uri]
}

// https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/
class GithubServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit config: GithubConfigService[F],
    csrfService: CsrfService[F],
    http: HttpWrapper[F]
) extends GithubService[F] {
  import GithubService._
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  def authenticateWithCallback(params: Map[String, String]): F[Unit] = {
    case class CodeState(code: String, state: String)

    val codeAndStateO = for {
      code  <- params.get("code")
      state <- params.get("state")
    } yield CodeState(code, state)

    for {
      codeAndState  <- codeAndStateO.pureOrRaise(new Exception())
      _             <- csrfService.checkCsrfToken(TYPE, codeAndState.state)
      tokenResponse <- getToken(codeAndState.code, codeAndState.state)
      apiResponse   <- getApi(tokenResponse)
      //TODO: user-db, jwt
    } yield ()
  }

  def generateIdentifierUrl: F[Uri] = {
    val githubUri = Uri("https://github.com/login/oauth/authorize")
    for {
      state    <- csrfService.getCsrfToken(TYPE)
      clientId <- config.getClientId
      scopes   <- config.getScopes //TODO: don't use scope as qparam if empty
      // TODO: maybe generate the redirect_uri
      // https://developer.github.com/apps/building-oauth-apps/authorizing-oauth-apps/#redirect-urls
    } yield
      githubUri.withQuery(
        Uri.Query("client_id" -> clientId, "scope" -> scopes.mkString(" "), "state" -> state)
      )
  }

  private def getToken(code: String, state: String): F[GithubTokenResponse] = {
    import spray.json._

    def createRequest(clientId: String, clientSecret: String) = {
      val postUrl = "https://github.com/login/oauth/access_token"
      val entity  = GithubTokenRequestDTO(clientId, clientSecret, code, state)
      HttpRequest(
        HttpMethods.POST,
        postUrl,
        Accept(MediaRange.One(MediaTypes.`application/json`, 1.0f)) :: Nil,
        HttpEntity(ContentTypes.`application/json`, entity.toJson.compactPrint)
      )
    }

    for {
      clientId     <- config.getClientId
      clientSecret <- config.getClientSecret
      response     <- http.singleRequest(createRequest(clientId, clientSecret))
      ret          <- http.unmarshalEntityTo[GithubTokenResponse](response)
    } yield ret
  }

  private def getApi(token: GithubService.GithubTokenResponse): F[IdAndRaw] = {
    val userEndpoint = "https://api.github.com/user"
    val request =
      HttpRequest(HttpMethods.GET, userEndpoint, RawHeader("Authorization", s"token ${token.access_token}") :: Nil)

    for {
      resp <- http.singleRequest(request)
      ret  <- http.unmarshalEntityTo[IdAndRaw](resp)
    } yield ret
  }

}

object GithubService {
  case class GithubTokenRequestDTO(client_id: String, client_secret: String, code: String, state: String)
  case class GithubTokenResponse(access_token: String)
  case class IdAndRaw(id: String, raw: JsValue)

  import spray.json.DefaultJsonProtocol._
  implicit val GithubTokenRequestDTOFormat: RootJsonFormat[GithubTokenRequestDTO] = jsonFormat4(GithubTokenRequestDTO)
  implicit val GithubTokenResponseFormat: RootJsonFormat[GithubTokenResponse]     = jsonFormat1(GithubTokenResponse)
  implicit val IdAndRawReader: RootJsonReader[IdAndRaw] = new RootJsonReader[IdAndRaw] {
    def read(value: JsValue): IdAndRaw = value match {
      case obj: JsObject =>
        obj.fields.get("id").fold(throw new Exception()) { id =>
          IdAndRaw(id.compactPrint, obj)
        }
      case _ => throw new Exception()
    }
  }
}
