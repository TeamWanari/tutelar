package com.wanari.tutelar.core.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.testkit.TestKit
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.HookService.{
  BasicAuthConfig,
  CustomHeaderAuthConfig,
  EscherAuthConfig,
  HookConfig,
  JwtAuthConfig
}
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{EscherService, HookService, JwtService}
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{AkkaHttpWrapper, HttpWrapper}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.any
import spray.json.{JsObject, JsString, JsTrue}

import scala.concurrent.Future
import scala.concurrent.duration.Duration

class HookServiceSpec extends TestKit(ActorSystem("HookServiceSpec")) with TestBase {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val userId     = "IDID"
  val externalId = "EXTID"
  val authType   = "AUTHTYPE"
  val userInfo   = JsObject("info" -> JsTrue)

  val responseData = JsObject("responseinfo" -> JsTrue)
  val response = HttpResponse(
    entity = HttpEntity(ContentTypes.`application/json`, responseData.compactPrint)
  )

  val baseUrl = "https://baseurl/path"

  val noUrlConfig = HookConfig(
    "",
    Seq("register", "login", "modify", "link", "unlink", "delete", "refresh"),
    BasicAuthConfig("user", "pass")
  )

  val noModulConfig = HookConfig(
    baseUrl,
    Seq.empty,
    BasicAuthConfig("user", "pass")
  )

  trait TestScope {
    import cats.instances.future._
    import system.dispatcher

    val httpMock = mock[HttpWrapper[Future]]
    when(httpMock.singleRequest(any[HttpRequest])(any[LogContext])) thenReturn Future.successful(response)

    implicit val dummyEscher = new EscherService[Future] {
      override def signRequest(service: String, request: HttpRequest): Future[HttpRequest] = {
        Future.successful(request.withHeaders(request.headers :+ RawHeader("ESCHER", service)))
      }
      override def init: Future[Unit] = ???
    }

    implicit val http = new AkkaHttpWrapper() {
      override def singleRequest(httpRequest: HttpRequest)(implicit ctx: LogContext): Future[HttpResponse] =
        httpMock.singleRequest(httpRequest)(ctx)
    }
    implicit def config =
      HookConfig(
        baseUrl,
        Seq("register", "login", "modify", "link", "unlink", "delete", "refresh"),
        BasicAuthConfig("user", "pass")
      )
    val jwtServiceMock = mock[JwtService[Future]]

    implicit def getJwtService(name: String): JwtConfig = ???
    lazy val service = new HookServiceImpl[Future]() {
      override lazy val jwtService = jwtServiceMock
    }
  }

  trait CustomHeaderTestScope extends TestScope {
    val customHeaderAuthConfig = CustomHeaderAuthConfig("Custom-Header-Auth", "top secret")
    override def config        = super.config.copy(authConfig = customHeaderAuthConfig)
  }

  trait EscherTestScope extends TestScope {
    override def config = super.config.copy(authConfig = EscherAuthConfig)
  }

  trait JwtTestScope extends TestScope {
    when(jwtServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn(Future.successful("TOKEN"))
    override def config = super.config.copy(authConfig = JwtAuthConfig)
  }

  type ServiceFunc = HookService[Future] => (String, String, String, JsObject) => Future[JsObject]
  Seq[(String, ServiceFunc)](
    "register" -> ((s: HookService[Future]) => s.register),
    "login"    -> ((s: HookService[Future]) => s.login),
    "link"     -> ((s: HookService[Future]) => s.link)
  ).foreach { case (name, getFunc) =>
    s"#$name" when {
      "call the backend" should {
        "add auth header - basic" in new TestScope {
          await(getFunc(service)(userId, externalId, authType, userInfo))
          validateBasicAuth(httpMock)
        }

        "add custom header - custom header" in new CustomHeaderTestScope {
          await(getFunc(service)(userId, externalId, authType, userInfo))
          validateCustomHeaderAuth(httpMock)
        }

        "sign request - escher" in new EscherTestScope {
          await(getFunc(service)(userId, externalId, authType, userInfo))
          validateEscherAuth(httpMock)
        }

        "add auth header - jwt" in new JwtTestScope {
          await(getFunc(service)(userId, externalId, authType, userInfo))
          validateJwtAuth(httpMock)
        }

        "send the user data and return the response" in new TestScope {
          await(getFunc(service)(userId, externalId, authType, userInfo)) shouldEqual responseData
          validateRequest(httpMock)(
            expectedUrl = s"$baseUrl/$name",
            expectedRequest = JsObject(
              "id"         -> JsString(userId),
              "externalId" -> JsString(externalId),
              "authType"   -> JsString(authType),
              "data"       -> userInfo
            )
          )
        }
      }
    }
  }

  "#modify" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.modify(userId, externalId, authType, userInfo))
        validateBasicAuth(httpMock)
      }

      "add custom header - custom header" in new CustomHeaderTestScope {
        await(service.modify(userId, externalId, authType, userInfo))
        validateCustomHeaderAuth(httpMock)
      }

      "sign request - escher" in new EscherTestScope {
        await(service.modify(userId, externalId, authType, userInfo))
        validateEscherAuth(httpMock)
      }

      "add auth header - jwt" in new JwtTestScope {
        await(service.modify(userId, externalId, authType, userInfo))
        validateJwtAuth(httpMock)
      }

      "send the user data" in new TestScope {
        await(service.modify(userId, externalId, authType, userInfo))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/modify",
          expectedRequest = JsObject(
            "id"         -> JsString(userId),
            "externalId" -> JsString(externalId),
            "authType"   -> JsString(authType),
            "data"       -> userInfo
          )
        )
      }
    }
  }

  "#unlink" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.unlink(userId, externalId, authType))
        validateBasicAuth(httpMock)
      }

      "add custom header - custom header" in new CustomHeaderTestScope {
        await(service.unlink(userId, externalId, authType))
        validateCustomHeaderAuth(httpMock)
      }

      "sign request - escher" in new EscherTestScope {
        await(service.unlink(userId, externalId, authType))
        validateEscherAuth(httpMock)
      }

      "add auth header - jwt" in new JwtTestScope {
        await(service.unlink(userId, externalId, authType))
        validateJwtAuth(httpMock)
      }

      "send the user id and auth type" in new TestScope {
        await(service.unlink(userId, externalId, authType))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/unlink",
          expectedRequest = JsObject(
            "id"         -> JsString(userId),
            "externalId" -> JsString(externalId),
            "authType"   -> JsString(authType)
          )
        )
      }
    }
  }

  "#delete" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.delete(userId))
        validateBasicAuth(httpMock)
      }

      "add custom header - custom header" in new CustomHeaderTestScope {
        await(service.delete(userId))
        validateCustomHeaderAuth(httpMock)
      }

      "sign request - escher" in new EscherTestScope {
        await(service.delete(userId))
        validateEscherAuth(httpMock)
      }

      "add auth header - jwt" in new JwtTestScope {
        await(service.delete(userId))
        validateJwtAuth(httpMock)
      }

      "send the user id" in new TestScope {
        await(service.delete(userId))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/delete",
          expectedRequest = JsObject("id" -> JsString(userId))
        )
      }
    }
  }

  "#refreshToken" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.refreshToken(userId, JsObject.empty))
        validateBasicAuth(httpMock)
      }

      "add custom header - custom header" in new CustomHeaderTestScope {
        await(service.refreshToken(userId, JsObject.empty))
        validateCustomHeaderAuth(httpMock)
      }

      "sign request - escher" in new EscherTestScope {
        await(service.refreshToken(userId, JsObject.empty))
        validateEscherAuth(httpMock)
      }

      "add auth header - jwt" in new JwtTestScope {
        await(service.refreshToken(userId, JsObject.empty))
        validateJwtAuth(httpMock)
      }

      "send the user id" in new TestScope {
        await(service.refreshToken(userId, JsObject("super" -> JsString("data"))))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/refresh",
          expectedRequest = JsObject("id" -> JsString(userId), "data" -> JsObject("super" -> JsString("data")))
        )
      }
    }
  }

  case class TestCaseWrapper[A](name: String, func: HookServiceImpl[Future] => Future[A], ret: A)
  Seq(
    TestCaseWrapper("register", s => s.register(userId, "", "", JsObject.empty), JsObject.empty),
    TestCaseWrapper("login", s => s.login(userId, "", "", JsObject.empty), JsObject.empty),
    TestCaseWrapper("modify", s => s.modify(userId, "", "", JsObject.empty), ()),
    TestCaseWrapper("link", s => s.link(userId, "", "", JsObject.empty), JsObject.empty),
    TestCaseWrapper("unlink", s => s.unlink(userId, "", ""), ()),
    TestCaseWrapper("delete", s => s.delete(userId), ()),
    TestCaseWrapper("refreshToken", s => s.refreshToken(userId, JsObject.empty), JsObject.empty)
  ).foreach { case TestCaseWrapper(name, func, ret) =>
    s"$name works if no baseUrl" in new TestScope {
      override lazy val config: HookConfig = noUrlConfig
      await(func(service)) shouldBe ret
      validateNoRequest(httpMock)
    }
    s"$name works if modulesDisabled" in new TestScope {
      override lazy val config: HookConfig = noModulConfig
      await(func(service)) shouldBe ret
      validateNoRequest(httpMock)
    }
  }

  def validateBasicAuth(httpMock: HttpWrapper[Future]): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())(any[LogContext])
    captor.getValue.getHeader("Authorization").get().value() shouldEqual "Basic dXNlcjpwYXNz"
  }

  def validateCustomHeaderAuth(httpMock: HttpWrapper[Future]): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())(any[LogContext])
    captor.getValue.getHeader("Custom-Header-Auth").get().value() shouldEqual "top secret"
  }

  def validateJwtAuth(httpMock: HttpWrapper[Future]): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())(any[LogContext])
    captor.getValue.getHeader("Authorization").get().value() shouldEqual "Bearer TOKEN"
  }

  def validateEscherAuth(httpMock: HttpWrapper[Future]): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())(any[LogContext])
    captor.getValue.getHeader("ESCHER").get().value() shouldEqual "hook"
  }

  def validateRequest(httpMock: HttpWrapper[Future])(expectedUrl: String, expectedRequest: Any): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())(any[LogContext])

    captor.getValue.uri.toString() shouldEqual expectedUrl
    captor.getValue.method shouldEqual HttpMethods.POST

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    await(Unmarshal(captor.getValue.entity).to[JsObject]) shouldEqual expectedRequest
  }

  def validateNoRequest(httpMock: HttpWrapper[Future]): Unit = {
    verify(httpMock, never).singleRequest(any[HttpRequest])(any[LogContext])
  }
}
