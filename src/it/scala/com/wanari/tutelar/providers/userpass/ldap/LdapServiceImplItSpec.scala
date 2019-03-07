package com.wanari.tutelar.providers.userpass.ldap

import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import org.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LdapServiceImplItSpec extends WordSpecLike with Matchers with AwaitUtil with MockitoSugar {

  private val services = new ItTestServices {
    import configService.runtimeConfig._
    override implicit lazy val ldapService: LdapService[Future] = new LdapServiceImpl()
  }

  "LdapService" should {
    "alice login" in {
      await(services.ldapService.login("alice", "alicepw"))
    }
    "bob login" in {
      await(services.ldapService.login("bob", "bobpw"))
    }
    "alice login failed" in {
      assertThrows[Exception] {
        await(services.ldapService.login("alice", "bobpw"))
      }
    }
  }
}
