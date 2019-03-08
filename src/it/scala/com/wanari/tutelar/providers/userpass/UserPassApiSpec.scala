package com.wanari.tutelar.providers.userpass

import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model._
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.ProviderApi._
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class UserPassApiSpec extends RouteTestBase {

  trait TestScope extends BaseTestScope {
    lazy val serviceMock = mock[UserPassService[Future]]
    override lazy val route = new UserPassApi {
      override val service        = serviceMock
      override val servicePath    = "testPath"
      override val callbackConfig = services.configService.runtimeConfig.callbackConfig
    }.route()
  }

  "GET /testPath/login" should {
    "forward the username and password to service" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/testPath/login?username=user&password=pw") ~> route ~> check {
        verify(serviceMock).login("user", "pw")
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/testPath/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?token=TOKEN")))
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.failed(new Exception())
      Get("/testPath/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?error=AUTHENTICATION_FAILED")))
      }
    }
  }

  "POST /testPath/login" should {
    val postLoginRequest = {
      val jsonRequest = LoginData("user", "pw").toJson.compactPrint
      val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)
      Post("/testPath/login").withEntity(entity)
    }

    "forward the username and password to service" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postLoginRequest ~> route ~> check {
        verify(serviceMock).login("user", "pw")
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postLoginRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("TOKEN")
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.login(any[String], any[String])) thenReturn Future.failed(new Exception())
      postLoginRequest ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }

  "GET /testPath/register" should {
    "forward the username and password to service" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/testPath/register?username=user&password=pw") ~> route ~> check {
        verify(serviceMock).register("user", "pw")
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/testPath/register?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?token=TOKEN")))
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.failed(new Exception())
      Get("/testPath/register?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?error=AUTHENTICATION_FAILED")))
      }
    }
  }

  "POST /testPath/register" should {
    val postregisterRequest = {
      val jsonRequest = LoginData("user", "pw").toJson.compactPrint
      val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)
      Post("/testPath/register").withEntity(entity)
    }

    "forward the username and password to service" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postregisterRequest ~> route ~> check {
        verify(serviceMock).register("user", "pw")
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postregisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("TOKEN")
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.register(any[String], any[String])) thenReturn Future.failed(new Exception())
      postregisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }

}
