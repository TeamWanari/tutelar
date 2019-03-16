package com.wanari.tutelar.core.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.HookService
import com.wanari.tutelar.core.HookService.{BasicAuthConfig, HookConfig}
import com.wanari.tutelar.util.{AkkaHttpWrapper, HttpWrapper}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentCaptor
import spray.json.{JsObject, JsString, JsTrue}

import scala.concurrent.Future

class HookServiceSpec extends TestKit(ActorSystem("HookServiceSpec")) with TestBase {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val mat = ActorMaterializer()

  val userId   = "IDID"
  val authType = "AUTHTYPE"
  val userInfo = JsObject("info" -> JsTrue)

  val responseData = JsObject("responseinfo" -> JsTrue)
  val response = HttpResponse(
    entity = HttpEntity(ContentTypes.`application/json`, responseData.compactPrint)
  )

  val baseUrl = "https://baseurl/path"

  trait TestScope {
    import cats.instances.future._
    import system.dispatcher

    val httpMock = mock[HttpWrapper[Future]]
    when(httpMock.singleRequest(any[HttpRequest])) thenReturn Future.successful(response)

    implicit val http = new AkkaHttpWrapper() {
      override def singleRequest(httpRequest: HttpRequest): Future[HttpResponse] = httpMock.singleRequest(httpRequest)
    }
    implicit val config = () => Future.successful(HookConfig(baseUrl, BasicAuthConfig("user", "pass")))
    lazy val service    = new HookServiceImpl[Future]()

  }

  type ServiceFunc = HookService[Future] => (String, String, JsObject) => Future[JsObject]
  Seq[(String, ServiceFunc)](
    "register" -> ((s: HookService[Future]) => s.register),
    "login"    -> ((s: HookService[Future]) => s.login),
    "link"     -> ((s: HookService[Future]) => s.link)
  ).foreach {
    case (name, getFunc) =>
      s"#$name" when {
        "call the backend" should {
          "add auth header - basic" in new TestScope {
            await(getFunc(service)(userId, authType, userInfo))
            validateBasicAuth(httpMock)
          }

          "send the user data and return the response" in new TestScope {
            await(getFunc(service)(userId, authType, userInfo)) shouldEqual responseData
            validateRequest(httpMock)(
              expectedUrl = s"$baseUrl/$name",
              expectedRequest = JsObject(
                "id"       -> JsString(userId),
                "authType" -> JsString(authType),
                "data"     -> userInfo
              )
            )
          }
        }
      }
  }

  "#modify" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.modify(userId, authType, userInfo))
        validateBasicAuth(httpMock)
      }

      "send the user data" in new TestScope {
        await(service.modify(userId, authType, userInfo))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/modify",
          expectedRequest = JsObject(
            "id"       -> JsString(userId),
            "authType" -> JsString(authType),
            "data"     -> userInfo
          )
        )
      }
    }
  }

  "#unlink" when {
    "call the backend" should {
      "add auth header - basic" in new TestScope {
        await(service.unlink(userId, authType))
        validateBasicAuth(httpMock)
      }

      "send the user id and auth type" in new TestScope {
        await(service.unlink(userId, authType))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/unlink",
          expectedRequest = JsObject(
            "id"       -> JsString(userId),
            "authType" -> JsString(authType)
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

      "send the user id" in new TestScope {
        await(service.delete(userId))
        validateRequest(httpMock)(
          expectedUrl = s"$baseUrl/delete",
          expectedRequest = JsObject("id" -> JsString(userId))
        )
      }
    }
  }

  def validateBasicAuth(httpMock: HttpWrapper[Future]): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())
    captor.getValue.getHeader("Authorization").get().value() shouldEqual "Basic dXNlcjpwYXNz"
  }

  def validateRequest(httpMock: HttpWrapper[Future])(expectedUrl: String, expectedRequest: Any): Unit = {
    val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
    verify(httpMock).singleRequest(captor.capture())

    captor.getValue.uri.toString() shouldEqual expectedUrl
    captor.getValue.method shouldEqual HttpMethods.POST

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json.DefaultJsonProtocol._
    await(Unmarshal(captor.getValue.entity).to[JsObject]) shouldEqual expectedRequest
  }

}