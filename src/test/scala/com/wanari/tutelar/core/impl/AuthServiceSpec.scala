package com.wanari.tutelar.core.impl

import cats.Id
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.{AuthConfigService, JwtService}
import com.wanari.tutelar.util.{DateTimeUtilCounterImpl, IdGeneratorCounterImpl}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{verify, when}
import spray.json.{JsObject, JsString}

class AuthServiceSpec extends TestBase {

  val authType     = "AUTH_TYPE"
  val externalId   = "EXT_ID"
  val customData   = "customData"
  val savedUser    = User("99999", 98765)
  val savedAccount = Account(authType, "SAVED_EXT_ID", savedUser.id, "somedata")

  trait TestScope {

    implicit val databaseService   = new DatabaseServiceMemImpl[Id]
    implicit val idGenerator       = new IdGeneratorCounterImpl[Id]
    implicit val timeService       = new DateTimeUtilCounterImpl[Id]
    implicit val jwtService        = mock[JwtService[Id]]
    implicit val authConfigService = mock[AuthConfigService[Id]]

    databaseService.saveUser(savedUser)
    databaseService.saveAccount(savedAccount)

    when(jwtService.encode(any[JsObject])).thenReturn("JWT")
    when(authConfigService.getCallbackUrl).thenReturn("http://url/?token=<<TOKEN>>")

    val service = new AuthServiceImpl[Id]()
  }

  "#registerOrLogin" when {
    "register" should {
      "return the callback url" in new TestScope {
        service.registerOrLogin(authType, externalId, customData) shouldEqual "http://url/?token=JWT"
      }
      "create token with the userid" in new TestScope {
        service.registerOrLogin(authType, externalId, customData)
        verify(jwtService).encode(JsObject("id" -> JsString("1")))
      }
      "create new user" in new TestScope {
        service.registerOrLogin(authType, externalId, customData)
        databaseService.users("1") shouldEqual User("1", 1)
      }
      "save the account" in new TestScope {
        service.registerOrLogin(authType, externalId, customData)
        databaseService.accounts((authType, externalId)) shouldEqual Account(authType, externalId, "1", customData)
      }
    }
    "login" should {
      "return the callback url" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData) shouldEqual "http://url/?token=JWT"
      }
      "create token with the userid" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData)
        verify(jwtService).encode(JsObject("id" -> JsString(savedUser.id)))
      }
      "update account custom data" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData)
        databaseService.accounts((savedAccount.authType, savedAccount.externalId)) shouldEqual Account(
          savedAccount.authType,
          savedAccount.externalId,
          savedAccount.userId,
          customData
        )
      }
    }
  }

}
