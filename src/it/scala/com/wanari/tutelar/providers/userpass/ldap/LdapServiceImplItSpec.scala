package com.wanari.tutelar.providers.userpass.ldap

import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import io.opentracing.noop.NoopTracerFactory
import org.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LdapServiceImplItSpec extends WordSpecLike with Matchers with AwaitUtil with MockitoSugar {

  private implicit lazy val dummyLogContext = {
    val tracer = NoopTracerFactory.create()
    val span   = tracer.buildSpan("test").start()
    new LogContext(tracer, span)
  }

  private val services = new ItTestServices {
    import configService.runtimeConfig._
    override implicit lazy val ldapService: LdapService[Future] = new LdapServiceImpl()
  }

  "LdapService" should {
    "alice login" in {
      await(services.ldapService.login("alice", "alicepw", None))
    }
    "bob login" in {
      await(services.ldapService.login("bob", "bobpw", None))
    }
    "alice login failed" in {
      assertThrows[Exception] {
        await(services.ldapService.login("alice", "bobpw", None))
      }
    }
  }
}
