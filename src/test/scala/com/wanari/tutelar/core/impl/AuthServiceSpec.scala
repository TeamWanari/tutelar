package com.wanari.tutelar.core.impl

import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.{HookService, JwtService}
import com.wanari.tutelar.util.{DateTimeUtilCounterImpl, IdGeneratorCounterImpl}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{verify, when}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration.Duration

class AuthServiceSpec extends TestBase {

  val authType             = "AUTH_TYPE"
  val externalId           = "EXT_ID"
  val customData           = "customData"
  val savedUser            = User("99999", 98765)
  val savedAccount         = Account(authType, "SAVED_EXT_ID", savedUser.id, "somedata")
  val providedData         = JsObject("userdata" -> JsString("helo"))
  val hookResponseLogin    = JsObject("group" -> JsString("log"))
  val hookResponseRegister = JsObject("group" -> JsString("reg"))

  trait TestScope {

    implicit val databaseService = new DatabaseServiceMemImpl[Id]
    implicit val idGenerator     = new IdGeneratorCounterImpl[Id]
    implicit val timeService     = new DateTimeUtilCounterImpl[Id]
    implicit val jwtService      = mock[JwtService[Id]]
    implicit val hookService     = mock[HookService[Id]]

    databaseService.saveUser(savedUser)
    databaseService.saveAccount(savedAccount)

    when(jwtService.encode(any[JsObject], any[Option[Duration]])).thenReturn("JWT")
    when(hookService.register(any[String], any[String], any[String], any[JsObject])).thenReturn(hookResponseRegister)
    when(hookService.login(any[String], any[String], any[String], any[JsObject])).thenReturn(hookResponseLogin)

    val service = new AuthServiceImpl[Id]()
  }

  "#registerOrLogin" when {
    "register" should {
      "return the token" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData) shouldEqual "JWT"
      }
      "create token with the userid and hook response" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData)
        verify(jwtService).encode(JsObject("id" -> JsString("1"), "group" -> JsString("reg")))
      }
      "create new user" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData)
        databaseService.users("1") shouldEqual User("1", 1)
      }
      "save the account" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData)
        databaseService.accounts((authType, externalId)) shouldEqual Account(authType, externalId, "1", customData)
      }
      "call hook service" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData)
        verify(hookService).register("1", externalId, authType, providedData)
      }
    }
    "login" should {
      "return the token" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData) shouldEqual "JWT"
      }
      "create token with the userid and hook response" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData)
        verify(jwtService).encode(JsObject("id" -> JsString(savedUser.id), "group" -> JsString("log")))
      }
      "update account custom data" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData)
        databaseService.accounts((savedAccount.authType, savedAccount.externalId)) shouldEqual Account(
          savedAccount.authType,
          savedAccount.externalId,
          savedAccount.userId,
          customData
        )
      }
      "call hook service" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData)
        verify(hookService).login(savedAccount.userId, savedAccount.externalId, authType, providedData)
      }
    }
  }

  "#findCustomData" when {
    "user found" in new TestScope {
      service.findCustomData(savedAccount.authType, savedAccount.externalId).value shouldEqual Some(
        savedAccount.customData
      )
    }
    "user not found" in new TestScope {
      service.findCustomData(authType, externalId).value shouldEqual None
    }
  }

}
