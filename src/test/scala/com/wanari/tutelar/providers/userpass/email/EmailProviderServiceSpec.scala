package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import org.mindrot.jbcrypt.BCrypt
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{verify, when}
import spray.json.{JsObject, JsString, JsTrue}

import scala.util.{Failure, Success, Try}

class EmailProviderServiceSpec extends TestBase {
  import cats.instances.try_._
  implicit val e: MonadError[Try, Throwable] = implicitly

  class TestScope extends ProviderTestScope[Try] {
    override lazy val authType        = "EMAIL"
    override lazy val savedCustomData = BCrypt.hashpw("secretpw", BCrypt.gensalt())

    implicit val configF      = () => Success(EmailProviderConfig("", "", "", "http://RegisterUrl?t=<<TOKEN>>"))
    implicit val emailService = mock[EmailServiceImpl[Try]]

    lazy val service = new EmailProviderServiceImpl[Try]()
  }

  "EmailProviderService" should {
    "#login" should {
      "successful" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "secretpw", None) shouldBe Success(jwtTokenResponse)
      }
      "send extra data via hook" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "secretpw", Some(JsObject("hello" -> JsTrue)))
        verify(hookService).login(savedAccount.userId, "EMAIL", JsObject("hello" -> JsTrue))
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
        when(jwtService.decode(any[String])).thenReturn(Success(JsObject("email" -> JsString("new@user"))))
        service.register("emailRegisterToken", "pw", None) shouldBe Success(jwtTokenResponse)

        verify(jwtService).decode("emailRegisterToken")

        val newUserData = databaseService.accounts.get(authType -> "new@user")
        newUserData shouldBe a[Some[_]]
        BCrypt.checkpw("pw", newUserData.get.customData) shouldBe true
      }
      "send extra data via hook" in new TestScope {
        when(jwtService.decode(any[String])).thenReturn(Success(JsObject("email" -> JsString("new@user"))))
        service.register("", "", Some(JsObject("hello"                           -> JsTrue)))

        verify(hookService).register(any[String], eqTo("EMAIL"), eqTo(JsObject("hello" -> JsTrue)))
      }
      "failure" when {
        "username is already used" in new TestScope {
          initDb()
          when(jwtService.decode(any[String]))
            .thenReturn(Success(JsObject("email" -> JsString(savedAccount.externalId))))
          service.register(savedAccount.externalId, "", None) shouldBe a[Failure[_]]
        }
        "token is wrong" in new TestScope {
          when(jwtService.decode(any[String])).thenReturn(Failure(new Exception))
          service.register("", "", Some(JsObject("hello" -> JsTrue))) shouldBe a[Failure[_]]
        }
      }
    }
    "#send-register" should {
      "call send service with the given email address" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Success(()))
        service.sendRegister("test@email") shouldBe Success(())
        verify(emailService).sendRegisterUrl(eqTo("test@email"), any[String])
      }
      "create token with the email address" in new TestScope {
        service.sendRegister("test@email")
        verify(jwtService).encode(JsObject("email" -> JsString("test@email")))
      }
      "create register url with token and call send service" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Success(()))
        when(jwtService.encode(any[JsObject])).thenReturn(Success("REG_TOKEN"))
        service.sendRegister("")
        verify(emailService).sendRegisterUrl(any[String], eqTo("http://RegisterUrl?t=REG_TOKEN"))
      }
      "fail when send failed" in new TestScope {
        when(emailService.sendRegisterUrl(any[String], any[String])).thenReturn(Failure(new Exception))
        service.sendRegister("") shouldBe a[Failure[_]]
      }
    }
  }
}
