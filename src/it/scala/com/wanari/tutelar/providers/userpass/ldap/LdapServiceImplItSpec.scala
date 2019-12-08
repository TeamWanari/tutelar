package com.wanari.tutelar.providers.userpass.ldap

import cats.data.EitherT
import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.Errors.AuthenticationFailed
import com.wanari.tutelar.providers.userpass.ldap.LdapService.LdapUserListData
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.{AwaitUtil, ItTestServices}
import io.opentracing.noop.NoopTracerFactory
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import spray.json.{JsArray, JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LdapServiceImplItSpec extends AnyWordSpecLike with Matchers with AwaitUtil with MockitoSugar {
  import cats.instances.future._

  private implicit lazy val dummyLogContext = {
    val tracer = NoopTracerFactory.create()
    val span   = tracer.buildSpan("test").start()
    new LogContext(tracer, span)
  }

  private val services = new ItTestServices {
    import configService._
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
    "list users with user id from tutelar db" in {
      val user1    = User("id1", 1)
      val user2    = User("id2", 2)
      val user3    = User("ldap_bob_id", 3)
      val account1 = Account("LDAP", "notbob", user1.id, "")
      val account2 = Account("type1", "alice", user2.id, "")
      val account3 = Account("LDAP", "bob", user3.id, "")
      val account4 = Account("type1", "ext3", user3.id, "")

      await(services.databaseService.saveUser(user1))
      await(services.databaseService.saveUser(user2))
      await(services.databaseService.saveUser(user3))
      await(services.databaseService.saveAccount(account1))
      await(services.databaseService.saveAccount(account2))
      await(services.databaseService.saveAccount(account3))
      await(services.databaseService.saveAccount(account4))

      await(services.ldapService.listUsers().value) shouldEqual Right(
        Seq(
          LdapUserListData(
            Some("ldap_bob_id"),
            Map(
              "givenName" -> JsString("Bob"),
              "memberOf" -> JsArray(
                JsString("cn=group1,ou=groups,dc=wanari,dc=com")
              ),
              "sn" -> JsString("Dilday"),
              "cn" -> JsString("bob")
            )
          ),
          LdapUserListData(
            None,
            Map(
              "givenName" -> JsString("Alice"),
              "memberOf" -> JsArray(
                JsString("cn=group1,ou=groups,dc=wanari,dc=com"),
                JsString("cn=group2,ou=groups,dc=wanari,dc=com")
              ),
              "sn" -> JsString("Smith"),
              "cn" -> JsString("alice")
            )
          )
        )
      )
    }
  }
}
