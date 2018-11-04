package com.wanari.tutelar.ldap

import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{StatusCodes, Uri}
import com.wanari.tutelar.RouteTestBase
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito._

import scala.concurrent.Future

class LdapApiSpec extends RouteTestBase {

  s"GET /ldap/login" should {
    "forward the username and password to service" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("CALLBACK")
      Get(s"/ldap/login?username=user&password=pw") ~> route ~> check {
        verify(services.ldapService).login("user", "pw")
      }
    }
    "return forward" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.successful("CALLBACK")
      Get(s"/ldap/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Found
        headers should contain(Location(Uri("CALLBACK")))
      }
    }
    "return with 401" in new BaseTestScope {
      when(services.ldapService.login(any[String], any[String])) thenReturn Future.failed(new Exception())
      Get(s"/ldap/login?username=user&password=pw") ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
  }

}
