package com.wanari.tutelar.providers.userpass.token

import java.security.SecureRandom

import cats.MonadError
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl._
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.{verify, when}
import spray.json._

import scala.util.{Failure, Success, Try}

class TotpServiceSpec extends TestBase {

  import cats.instances.try_._
  implicit val e: MonadError[Try, Throwable] = implicitly

  class TestScope extends ProviderTestScope[Try] {
    override lazy val authType = "TOTP"
    override lazy val savedCustomData =
      TotpData("SHA1", 6, 30000, 0, "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ").toJson.toString

    implicit val configF = () => Success(TotpConfig("SHA1", 1, 30000, 6, true))
    val random           = mock[SecureRandom]

    lazy val service = new TotpServiceImpl[Try]() {
      protected override def now: Long                  = 0
      protected override val secureRandom: SecureRandom = random
    }
  }

  "qrCodeData" should {
    "generate a valid token with url" in new TestScope {
      service.qrCodeData shouldBe Success(
        QRData(
          "JWT",
          "otpauth://totp/?digits=6&algorithm=SHA1&period=30000&secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        )
      )
    }
  }

  "register" should {
    def initMock(jwtService: JwtService[Try]) = {
      when(jwtService.validateAndDecode(any[String])).thenReturn(
        Success(
          TotpData("SHA1", 6, 30000, 0, "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ").toJson.asJsObject
        )
      )
    }
    "successful" in new TestScope {
      initMock(jwtService)

      service.register("newUser", "token", "755224", None)
      verify(jwtService).validateAndDecode("token")

      val newUserData = databaseService.accounts.get(authType -> "newUser")
      newUserData shouldBe a[Some[_]]
      newUserData.get.customData shouldBe savedCustomData
    }

    "sends extra data via hook" in new TestScope {
      initMock(jwtService)
      service.register("newUser", "token", "755224", Some(JsObject("hello" -> JsTrue)))

      verify(hookService).register(any[String], eqTo("newUser"), eqTo("TOTP"), eqTo(JsObject("hello" -> JsTrue)))
    }

    "failure" when {
      "username is already used" in new TestScope {
        initDb()
        service.register(savedAccount.externalId, "token", "755224", None) shouldBe a[Failure[_]]
      }
      "the incoming token is not valid" in new TestScope {
        when(jwtService.validateAndDecode(any[String])).thenReturn(Failure(new Exception))
        service.register("newUser", "token", "755224", None) shouldBe a[Failure[_]]
      }

      "the token + pass pair not match" in new TestScope {
        initMock(jwtService)
        service.register("newUser", "token", "755225", None) shouldBe a[Failure[_]]
      }
    }

    "#login" should {
      "successful" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "755224", None) shouldBe Success(jwtTokenResponse)
      }
      "send extra data via hook" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "755224", Some(JsObject("hello" -> JsTrue)))
        verify(hookService).login(savedAccount.userId, savedAccount.externalId, "TOTP", JsObject("hello" -> JsTrue))
      }
      "failure" when {
        "user not found" in new TestScope {
          service.login(savedAccount.externalId, "755224", None) shouldBe a[Failure[_]]
        }
        "wrong password" in new TestScope {
          initDb()
          service.login(savedAccount.externalId, "755225", None) shouldBe a[Failure[_]]
        }
      }
    }
  }
}
