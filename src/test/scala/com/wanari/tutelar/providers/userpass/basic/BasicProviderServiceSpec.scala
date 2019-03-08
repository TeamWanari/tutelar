package com.wanari.tutelar.providers.userpass.basic
import cats.MonadError
import com.wanari.tutelar.TestBase
import org.mindrot.jbcrypt.BCrypt

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
      service.login(savedAccount.externalId, "secretpw") shouldBe Success(jwtTokenResponse)
    }
    "failure" when {
      "user not found" in new TestScope {
        service.login(savedAccount.externalId, "secretpw") shouldBe a[Failure[_]]
      }
      "wrong password" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "wrongpw") shouldBe a[Failure[_]]
      }
    }
  }
  "#register" should {
    "successful" in new TestScope {
      service.register("newUser", "pw") shouldBe Success(jwtTokenResponse)

      val newUserData = databaseService.accounts.get(authType -> "newUser")
      newUserData shouldBe a[Some[_]]
      BCrypt.checkpw("pw", newUserData.get.customData) shouldBe true
    }
    "failure" when {
      "username is already used" in new TestScope {
        initDb()
        service.register(savedAccount.externalId, "asd")
      }
    }
  }
}
