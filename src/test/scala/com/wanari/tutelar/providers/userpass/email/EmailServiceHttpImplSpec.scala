package com.wanari.tutelar.providers.userpass.email

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cats.MonadError
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito._
import spray.json.{JsObject, JsString}

import scala.util.{Failure, Success, Try}

class EmailServiceHttpImplSpec extends TestKit(ActorSystem("HookServiceSpec")) with TestBase {
  import cats.instances.try_._

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val mat = ActorMaterializer()

  trait TestScope {
    implicit lazy val e: MonadError[Try, Throwable] = implicitly
    implicit lazy val httpWrapper                   = mock[HttpWrapper[Try]]
    when(httpWrapper.singleRequest(any[HttpRequest])(any[LogContext])).thenReturn(Failure(new Exception))
    implicit lazy val config: EmailServiceHttpConfig = EmailServiceHttpConfig("_SERVICE_URL_", "_USER_", "_PASS_")
    lazy val service                                 = new EmailServiceHttpImpl[Try]()
  }

  "EmailServiceHttpImpl" should {
    "sendRegister" should {
      "call the email service with credentials" in new TestScope {
        service.sendRegisterUrl("", "")

        val expectedUrl = "_SERVICE_URL_/send"

        val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        verify(httpWrapper).singleRequest(captor.capture())(any[LogContext])

        captor.getValue.uri.toString() shouldEqual expectedUrl
        captor.getValue.method shouldEqual HttpMethods.POST
        captor.getValue.headers should contain(Authorization(BasicHttpCredentials("_USER_", "_PASS_")))
      }
      "send the correct data" in new TestScope {
        service.sendRegisterUrl("to@test", "RegisterTOKEN")

        val expectedRequest = JsObject(
          "email"          -> JsString("to@test"),
          "templateId"     -> JsString("register"),
          "titleArguments" -> JsObject(),
          "bodyArguments"  -> JsObject("token" -> JsString("RegisterTOKEN"))
        )

        val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        verify(httpWrapper).singleRequest(captor.capture())(any[LogContext])

        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        await(Unmarshal(captor.getValue.entity).to[JsObject]) shouldEqual expectedRequest
      }
      "return Success if result is successful" in new TestScope {
        reset(httpWrapper)
        when(httpWrapper.singleRequest(any[HttpRequest])(any[LogContext])).thenReturn(Success(HttpResponse()))
        service.sendRegisterUrl("", "") shouldEqual Success(())
      }
      "return Failure if result is not successful" in new TestScope {
        reset(httpWrapper)
        when(httpWrapper.singleRequest(any[HttpRequest])(any[LogContext]))
          .thenReturn(Success(HttpResponse(StatusCodes.BadRequest)))
        service.sendRegisterUrl("", "") shouldBe a[Failure[_]]
      }
    }
    "sendResetPasswordUrl" should {
      "call the email service with credentials" in new TestScope {
        service.sendResetPasswordUrl("", "")

        val expectedUrl = "_SERVICE_URL_/send"

        val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        verify(httpWrapper).singleRequest(captor.capture())(any[LogContext])

        captor.getValue.uri.toString() shouldEqual expectedUrl
        captor.getValue.method shouldEqual HttpMethods.POST
        captor.getValue.headers should contain(Authorization(BasicHttpCredentials("_USER_", "_PASS_")))
      }
      "send the correct data" in new TestScope {
        service.sendResetPasswordUrl("to@test", "ResetTOKEN")

        val expectedRequest = JsObject(
          "email"          -> JsString("to@test"),
          "templateId"     -> JsString("reset-password"),
          "titleArguments" -> JsObject(),
          "bodyArguments"  -> JsObject("token" -> JsString("ResetTOKEN"))
        )

        val captor: ArgumentCaptor[HttpRequest] = ArgumentCaptor.forClass(classOf[HttpRequest])
        verify(httpWrapper).singleRequest(captor.capture())(any[LogContext])

        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        await(Unmarshal(captor.getValue.entity).to[JsObject]) shouldEqual expectedRequest
      }
      "return Success if result is successful" in new TestScope {
        reset(httpWrapper)
        when(httpWrapper.singleRequest(any[HttpRequest])(any[LogContext])).thenReturn(Success(HttpResponse()))
        service.sendResetPasswordUrl("", "") shouldEqual Success(())
      }
      "return Failure if result is not successful" in new TestScope {
        reset(httpWrapper)
        when(httpWrapper.singleRequest(any[HttpRequest])(any[LogContext]))
          .thenReturn(Success(HttpResponse(StatusCodes.BadRequest)))
        service.sendResetPasswordUrl("", "") shouldBe a[Failure[_]]
      }
    }
  }
}
