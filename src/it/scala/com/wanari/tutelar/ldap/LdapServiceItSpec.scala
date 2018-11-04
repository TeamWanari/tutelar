package com.wanari.tutelar.ldap

import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import org.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LdapServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with MockitoSugar {

  private val services = new ItTestServices {
    implicit lazy val ldapConfig: LdapConfigService[Future]     = configService.getLdapConfig
    override implicit lazy val ldapService: LdapService[Future] = new LdapServiceImpl()
  }

  "LdapService" should {
    "alice login" in {
      await(services.ldapService.login("alice", "alicepw")) should startWith("https://localhost/login?token=")
    }
    "bob login" in {
      await(services.ldapService.login("bob", "bobpw")) should startWith("https://localhost/login?token=")
    }
    "alice login failed" in {
      assertThrows[Exception] {
        await(services.ldapService.login("alice", "bobpw"))
      }
    }
  }
}
