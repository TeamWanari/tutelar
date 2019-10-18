package com.wanari.tutelar.providers.userpass.basic

import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import com.wanari.tutelar.RouteTestBase
import com.wanari.tutelar.core.ProviderApi._
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import cats.data.EitherT
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.AuthenticationFailed
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentMatchersSugar._

import scala.concurrent.Future

class BasicProviderApiSpec extends RouteTestBase {
  import cats.instances.future._

  trait TestScope extends BaseTestScope {
    implicit lazy val serviceMock    = mock[BasicProviderService[Future]]
    implicit lazy val callbackConfig = services.configService.getCallbackConfig
    override lazy val route          = new BasicProviderApi().route()
  }

  val jsonRequest = LoginData("user", "pw", Some(JsObject("hello" -> JsTrue)), None).toJson.compactPrint
  val entity      = HttpEntity(MediaTypes.`application/json`, jsonRequest)

  "POST /basic/register" should {
    val postRegisterRequest = {
      Post("/basic/register").withEntity(entity)
    }

    "forward the username, password and extra data to service" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])(any[LogContext])) thenReturn EitherT
        .rightT(TokenData("TOKEN", "REFRESH_TOKEN"))
      postRegisterRequest ~> route ~> check {
        verify(serviceMock).register(eqTo("user"), eqTo("pw"), eqTo(Some(JsObject("hello" -> JsTrue))))(any[LogContext])
      }
    }
    "return redirect with callback" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])(any[LogContext])) thenReturn EitherT
        .rightT(TokenData("TOKEN", "REFRESH_TOKEN"))
      postRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[TokenData] shouldEqual TokenData("TOKEN", "REFRESH_TOKEN")
      }
    }
    "return redirect with error" in new TestScope {
      when(serviceMock.register(any[String], any[String], any[Option[JsObject]])(any[LogContext])) thenReturn EitherT
        .leftT(AuthenticationFailed())
      postRegisterRequest ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
        responseAs[ErrorData] shouldEqual ErrorData("AUTHENTICATION_FAILED")
      }
    }
  }
}
