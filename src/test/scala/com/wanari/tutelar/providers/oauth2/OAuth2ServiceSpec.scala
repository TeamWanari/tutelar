package com.wanari.tutelar.providers.oauth2

import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.{AuthService, CsrfService}
import com.wanari.tutelar.providers.oauth2.OAuth2Service.{ProfileData, TokenRequestHelper, TokenResponseHelper}
import com.wanari.tutelar.util.HttpWrapper
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito._
import spray.json.JsString

import scala.util.{Failure, Try}

class OAuth2ServiceSpec extends TestBase {

  "TokenRequestHelper" should {
    "create jsonEntity" in withActorSystem { implicit actorSystem =>
      implicit val materializer = ActorMaterializer()
      val trh                   = TokenRequestHelper("a", "b", "c", "d")
      val jsonEntity            = trh.jsonEntity
      jsonEntity.contentType shouldBe ContentTypes.`application/json`
      await(jsonEntity.httpEntity.toStrict(timeout)).data.utf8String shouldBe
        """{"client_id":"a","client_secret":"b","code":"c","state":"d"}"""
    }

    "create map" in {
      val trh = TokenRequestHelper("a", "b", "c", "d")
      val map = trh.getAsMap("uri")
      map.keys.toList should contain allOf ("client_id", "client_secret", "code", "state", "redirect_uri")
      map.toList should contain("grant_type" -> "authorization_code")
    }

    "create formEntity" in withActorSystem { implicit actorSystem =>
      implicit val materializer = ActorMaterializer()
      val trh                   = TokenRequestHelper("a", "b", "c", "d")
      val formEntity            = trh.formEntity("uri")
      formEntity.contentType shouldBe (MediaTypes.`application/x-www-form-urlencoded` withCharset HttpCharsets.`UTF-8`)
      await(formEntity.httpEntity.toStrict(timeout)).data.utf8String shouldBe
        """state=d&redirect_uri=uri&client_id=a&code=c&client_secret=b&grant_type=authorization_code"""
    }
  }

  "idAndRawReader" should {
    import OAuth2Service._
    import spray.json._
    "parse out the given field (id)" in {
      val jsonStr       = """{"id": "test", "data":"sth"}"""
      val convertedJson = jsonStr.parseJson.convertTo[ProfileData](profileDataReader("id"))

      convertedJson.id shouldBe "test"
      convertedJson.data shouldBe jsonStr.parseJson
    }

    "parse out the given field (data)" in {
      val jsonStr       = """{"id": "test", "data":"sth"}"""
      val convertedJson = jsonStr.parseJson.convertTo[ProfileData](profileDataReader("data"))

      convertedJson.id shouldBe "sth"
      convertedJson.data shouldBe jsonStr.parseJson
    }

    "parse out nonstrings" in {
      val jsonStr       = """{"id": [1,2], "data":"sth"}"""
      val convertedJson = jsonStr.parseJson.convertTo[ProfileData](profileDataReader("id"))

      convertedJson.id shouldBe "[1,2]"
      convertedJson.data shouldBe jsonStr.parseJson
    }

    "fails if the given id field is not in the json" in {
      val jsonStr       = """{"id": "test", "data":"sth"}"""
      val convertedJson = Try(jsonStr.parseJson.convertTo[ProfileData](profileDataReader("sub")))

      convertedJson shouldBe a[Failure[_]]
    }
  }

  "OAuth2Service" should {
    import cats.instances.try_._

    trait Scope {
      val service = new OAuth2Service[Try] {
        val config      = mock[OAuth2ConfigService[Try]]
        val csrfService = mock[CsrfService[Try]]
        val http        = mock[HttpWrapper[Try]]
        val authService = mock[AuthService[Try]]

        val TYPE: String         = "dummy"
        val redirectUriBase: Uri = Uri("https://example.com")
        override def createTokenRequest(entityHelper: TokenRequestHelper, selfRedirectUri: Uri) = {
          HttpRequest()
        }
        override def getProfile(token: TokenResponseHelper) =
          if (token.access_token == "token") Try(ProfileData("id", JsString("raw"))) else ???
      }
    }

    "generateIdentifierUrl correctly" in new Scope {
      when(service.config.getClientId) thenReturn Try("clientId")
      when(service.config.getScopes) thenReturn Try(Seq("a", "b"))
      when(service.config.getRootUrl) thenReturn Try("https://self.com/test")
      when(service.csrfService.getCsrfToken("dummy")) thenReturn Try("csrf")

      service.generateIdentifierUrl.get shouldBe Uri(
        "https://example.com?client_id=clientId&scope=a+b&state=csrf&response_type=code&redirect_uri=https://self.com/test/dummy/callback"
      )
    }

    "authenticateWithCallback correctly" in new Scope {
      when(service.config.getClientId) thenReturn Try("clientId")
      when(service.config.getClientSecret) thenReturn Try("clientSecret")
      when(service.config.getRootUrl) thenReturn Try("https://self.com/test")
      when(service.csrfService.checkCsrfToken("dummy", "state")) thenReturn Try({})
      when(service.authService.registerOrLogin("dummy", "id", "token")) thenReturn Try("ultimateUri")
      when(service.http.singleRequest(any[HttpRequest])) thenReturn Try(HttpResponse())
      when(service.http.unmarshalEntityTo[TokenResponseHelper](HttpResponse())) thenReturn Try(
        TokenResponseHelper("token")
      )

      service.authenticateWithCallback("code", "state").get shouldBe "ultimateUri"
    }
  }

}
