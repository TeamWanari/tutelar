package com.wanari.tutelar.providers.userpass.basic
import cats.MonadError
import com.wanari.tutelar.TestBase
import org.mindrot.jbcrypt.BCrypt
import org.mockito.Mockito.verify
import spray.json.{JsObject, JsTrue}
import org.mockito.ArgumentMatchersSugar._

import scala.util.{Failure, Success, Try}

class BasicProviderServiceSpec extends TestBase {

  import cats.instances.try_._

  implicit val e: MonadError[Try, Throwable] = implicitly

  class TestScope extends ProviderTestScope[Try] {
    override lazy val authType        = "BASIC"
    override lazy val savedCustomData = BCrypt.hashpw("secretpw", BCrypt.gensalt())

    val service = new BasicProviderServiceImpl[Try]()
  }

  "#login" should {
    "successful" in new TestScope {
      initDb()
      service.login(savedAccount.externalId, "secretpw", None) shouldBe Success(jwtTokenResponse)
    }
    "send extra data via hook" in new TestScope {
      initDb()
      service.login(savedAccount.externalId, "secretpw", Some(JsObject("hello" -> JsTrue)))
      verify(hookService).login(savedAccount.userId, savedAccount.externalId, "BASIC", JsObject("hello" -> JsTrue))
    }
    "failure" when {
      "user not found" in new TestScope {
        service.login(savedAccount.externalId, "secretpw", None) shouldBe a[Failure[_]]
      }
      "wrong password" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "wrongpw", None) shouldBe a[Failure[_]]
      }
    }
  }
  "#register" should {
    "successful" in new TestScope {
      service.register("newUser", "pw", None) shouldBe Success(jwtTokenResponse)

      val newUserData = databaseService.accounts.get(authType -> "newUser")
      newUserData shouldBe a[Some[_]]
      BCrypt.checkpw("pw", newUserData.get.customData) shouldBe true
    }
    "send extra data via hook" in new TestScope {
      service.register("newUser", "pw", Some(JsObject("hello" -> JsTrue)))

      verify(hookService).register(any[String], eqTo("newUser"), eqTo("BASIC"), eqTo(JsObject("hello" -> JsTrue)))
    }
    "failure" when {
      "username is already used" in new TestScope {
        initDb()
        service.register(savedAccount.externalId, "asd", None) shouldBe a[Failure[_]]
      }
    }
  }
}
