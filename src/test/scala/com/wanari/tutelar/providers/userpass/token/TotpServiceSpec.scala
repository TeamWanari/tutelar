package com.wanari.tutelar.providers.userpass.token

import java.security.SecureRandom

import cats.MonadError
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl._
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.{verify, when}
import spray.json._

import scala.concurrent.duration.Duration
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

    val jwtServiceMock = mock[JwtService[Try]]

    lazy val service = new TotpServiceImpl[Try]() {
      protected override def now: Long                  = 0
      protected override val secureRandom: SecureRandom = random

      protected override val jwtService = jwtServiceMock
    }
  }

  "qrCodeData" should {
    "generate a valid token with url" in new TestScope {
      when(jwtServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn(Success("JWT"))

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
      initMock(jwtServiceMock)

      service.register("newuser", "token", "755224", None)
      verify(jwtServiceMock).validateAndDecode("token")

      val newuserData = databaseService.accounts.get(authType -> "newuser")
      newuserData shouldBe a[Some[_]]
      newuserData.get.customData shouldBe savedCustomData
    }

    "sends extra data via hook" in new TestScope {
      initMock(jwtServiceMock)
      service.register("newuser", "token", "755224", Some(JsObject("hello" -> JsTrue)))

      verify(hookService).register(any[String], eqTo("newuser"), eqTo("TOTP"), eqTo(JsObject("hello" -> JsTrue)))(
        any[LogContext]
      )
    }

    "failure" when {
      "username is already used" in new TestScope {
        initDb()
        service.register(savedAccount.externalId, "token", "755224", None) shouldBe a[Failure[_]]
      }
      "the incoming token is not valid" in new TestScope {
        when(jwtServiceMock.validateAndDecode(any[String])).thenReturn(Failure(new Exception))
        service.register("newuser", "token", "755224", None) shouldBe a[Failure[_]]
      }

      "the token + pass pair not match" in new TestScope {
        initMock(jwtServiceMock)
        service.register("newuser", "token", "755225", None) shouldBe a[Failure[_]]
      }
    }

    "#login" should {
      "successful" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "755224", None) shouldBe Success(authenticateResponse)
      }
      "send extra data via hook" in new TestScope {
        initDb()
        service.login(savedAccount.externalId, "755224", Some(JsObject("hello" -> JsTrue)))
        verify(hookService).login(
          eqTo(savedAccount.userId),
          eqTo(savedAccount.externalId),
          eqTo("TOTP"),
          eqTo(JsObject("hello" -> JsTrue))
        )(any[LogContext])
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