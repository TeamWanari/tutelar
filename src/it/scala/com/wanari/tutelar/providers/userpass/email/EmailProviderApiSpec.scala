package com.wanari.tutelar.providers.userpass.email

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.ProviderApi.{ErrorData, TokenData}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.wanari.tutelar.providers.userpass.email.EmailProviderApi.{EmailData, RegisterData}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito._

import scala.concurrent.Future

class EmailProviderApiSpec extends RouteTestBase {

  trait TestScope extends BaseTestScope {
    implicit lazy val serviceMock    = mock[EmailProviderService[Future]]
    implicit lazy val callbackConfig = services.configService.runtimeConfig.callbackConfig
    override lazy val route          = new EmailProviderApi().route()
  }

  "POST /email/register" should {
    val postRegisterRequest = {
      val jsonRequest = RegisterData("registerToken", "pw", Some(JsObject("hello" -> JsTrue))).toJson.compactPrint
      val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)
      Post("/email/register").withEntity(entity)
    }

    "forward the username, password and extra data to service" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])) thenReturn Future.successful("TOKEN")
      postRegisterRequest ~> route ~> check {
        verify(serviceMock).register("registerToken", "pw", Some(JsObject("hello" -> JsTrue)))
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])) thenReturn Future.successful("TOKEN")
      postRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("TOKEN")
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])) thenReturn Future.failed(
        new Exception()
      )
      postRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }
  "POST /email/send-register" should {
    val postSendRegisterRequest = {
      val jsonRequest = EmailData("email").toJson.compactPrint
      val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)
      Post("/email/send-register").withEntity(entity)
    }

    "forward the email address to service" in new TestScope {
      when(serviceMock.sendRegister(any[String])) thenReturn Future.successful(())
      postSendRegisterRequest ~> route ~> check {
        verify(serviceMock).sendRegister("email")
      }
    }
    "return ok" in new TestScope {
      when(serviceMock.sendRegister(any[String])) thenReturn Future.successful(())
      postSendRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "return error" in new TestScope {
      when(serviceMock.sendRegister(any[String])) thenReturn Future.failed(new Exception())
      postSendRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.InternalServerError
      }
    }
  }
}
