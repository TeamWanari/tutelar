package com.wanari.tutelar.providers.userpass.basic
import cats.MonadError
import cats.data.EitherT
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.Errors.{AuthenticationFailed, UserNotFound, UsernameUsed, WeakPassword}
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.NonEmptyPasswordChecker
import org.mindrot.jbcrypt.BCrypt
import org.mockito.ArgumentMatchersSugar._
import spray.json.{JsObject, JsTrue}

import scala.util.Try

class BasicProviderServiceSpec extends TestBase {
  import cats.instances.try_._

  implicit val e: MonadError[Try, Throwable] = implicitly

  class TestScope extends ProviderTestScope[Try] {
    override lazy val authType        = "BASIC"
    override lazy val savedCustomData = BCrypt.hashpw("secretpw", BCrypt.gensalt())

    implicit val passwordChecker = new NonEmptyPasswordChecker[Try]

    val service = new BasicProviderServiceImpl[Try]()
  }

  "#login" should {
    "successful" in new TestScope {
      initDb()
      service.login(savedAccount.externalId, "secretpw", None, None) shouldBe EitherT.rightT(authenticateResponse)
    }
    "send extra data via hook" in new TestScope {
      initDb()
      service.login(savedAccount.externalId, "secretpw", Some(JsObject("hello" -> JsTrue)), None).value.get
      verify(hookService).login(
        eqTo(savedAccount.userId),
        eqTo(savedAccount.externalId),
        eqTo("BASIC"),
        eqTo(JsObject("hello" -> JsTrue))
      )(any[LogContext])
    }
    "failure" when {
      "user not found" in new TestScope {
        service.login(savedAccount.externalId, "secretpw", None, None) shouldBe EitherT.leftT(UserNotFound())
      }
      "wrong password" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "wrongpw", None, None) shouldBe EitherT.leftT(AuthenticationFailed())
      }
    }
  }
  "#register" should {
    "successful" in new TestScope {
      service.register("newuser", "pw", None, None) shouldBe EitherT.rightT(authenticateResponse)

      val newUserData = databaseService.accounts.get(authType -> "newuser")
      newUserData shouldBe a[Some[_]]
      BCrypt.checkpw("pw", newUserData.get.customData) shouldBe true
    }
    "send extra data via hook" in new TestScope {
      service.register("newuser", "pw", Some(JsObject("hello" -> JsTrue)), None)

      verify(hookService).register(any[String], eqTo("newuser"), eqTo("BASIC"), eqTo(JsObject("hello" -> JsTrue)))(
        any[LogContext]
      )
    }
    "failure" when {
      "password is weak" in new TestScope {
        service.register("newuser", "", None, None) shouldBe EitherT.leftT(WeakPassword())
      }
      "username is already used" in new TestScope {
        initDb()
        service.register(savedAccount.externalId, "asd", None, None) shouldBe EitherT.leftT(UsernameUsed())
      }
    }
  }
}
