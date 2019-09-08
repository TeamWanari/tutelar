package com.wanari.tutelar.providers.userpass.ldap

import cats.data.EitherT
import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.AuthenticationFailed
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import io.opentracing.noop.NoopTracerFactory
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import spray.json.JsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LdapServiceImplItSpec extends WordSpecLike with Matchers with AwaitUtil with MockitoSugar {
  import cats.instances.future._

  private implicit lazy val dummyLogContext = {
    val tracer = NoopTracerFactory.create()
    val span   = tracer.buildSpan("test").start()
    new LogContext(tracer, span)
  }

  private val services = new ItTestServices {
    import configService.runtimeConfig._
    override implicit lazy val authService: AuthService[Future] = mock[AuthService[Future]]
    when(authService.registerOrLogin(any[String], any[String], any[String], any[JsObject])(any[LogContext])) thenReturn EitherT
      .rightT(TokenData("TOKEN", "REFRESH_TOKEN"))
    override implicit lazy val ldapService: LdapService[Future] = new LdapServiceImpl()
  }

  "LdapService" should {
    "alice login" in {
      await(services.ldapService.login("alice", "alicepw", None).value) shouldEqual Right(
        TokenData("TOKEN", "REFRESH_TOKEN")
      )
    }
    "bob login" in {
      await(services.ldapService.login("bob", "bobpw", None).value) shouldEqual Right(
        TokenData("TOKEN", "REFRESH_TOKEN")
      )
    }
    "alice login failed" in {
      await(services.ldapService.login("alice", "bobpw", None).value) shouldEqual Left(
        AuthenticationFailed()
      )
    }
  }
}
