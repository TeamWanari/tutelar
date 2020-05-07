package com.wanari.tutelar.providers.userpass.ldap

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.server.AuthenticationFailedRejection
import cats.data.EitherT
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.providers.userpass.ldap.LdapService.LdapUserListData
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentMatchersSugar._

import scala.concurrent.Future

class LdapProviderApiSpec extends RouteTestBase {
  import cats.instances.future._

  trait TestScope extends BaseTestScope {
    import services.configService._
    implicit lazy val serviceMock = mock[LdapService[Future]]
    override lazy val route       = new LdapApi().route()
  }
  "GET /ldap/users" should {
    "error without auth" in new TestScope {
      Get("/ldap/users") ~> route ~> check {
        rejection shouldBe an[AuthenticationFailedRejection]
      }
    }
    "return the users list" in new TestScope {
      import LdapApi._
      import spray.json.DefaultJsonProtocol._
      import spray.json._
      val serviceResult: Seq[LdapUserListData] = Seq(
        LdapUserListData(None, Map("a" -> JsString("b"), "c" -> JsNumber(1))),
        LdapUserListData(Some("_id_"), Map("d" -> JsObject("e" -> JsTrue)))
      )
      when(serviceMock.listUsers()(any[LogContext])) thenReturn EitherT.rightT(serviceResult)

      val validCredentials = BasicHttpCredentials("testuser", "testpass")
      Get("/ldap/users") ~> addCredentials(validCredentials) ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[Seq[LdapUserListData]] shouldEqual serviceResult
      }
    }
  }
}
