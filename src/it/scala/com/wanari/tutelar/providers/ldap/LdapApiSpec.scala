package com.wanari.tutelar.providers.ldap

import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model._
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.ProviderApi._
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.Future

class LdapApiSpec extends RouteTestBase {

  s"GET /ldap/login" should {
    "forward the username and password to service" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/ldap/login?username=user&password=pw") ~> route ~> check {
        verify(services.ldapService).login("user", "pw")
      }
    }
    "return redirect with callback" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      Get("/ldap/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?token=TOKEN")))
      }
    }
    "return redirect with error" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.failed(new Exception())
      Get("/ldap/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("https://lvh.me:9443/index.html?error=AUTHENTICATION_FAILED")))
      }
    }
  }

  "POST /ldap/login" should {
    val postLoginRequest = {
      val jsonRequest = LoginData("user", "pw").toJson.compactPrint
      val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)
      Post("/ldap/login").withEntity(entity)
    }

    "forward the username and password to service" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postLoginRequest ~> route ~> check {
        verify(services.ldapService).login("user", "pw")
      }
    }
    "return redirect with callback" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("TOKEN")
      postLoginRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("TOKEN")
      }
    }
    "return redirect with error" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.failed(new Exception())
      postLoginRequest ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }

}
