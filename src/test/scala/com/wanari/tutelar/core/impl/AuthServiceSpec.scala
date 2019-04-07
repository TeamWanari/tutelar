package com.wanari.tutelar.core.impl

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.impl.database.MemoryDatabaseService
import com.wanari.tutelar.core.{HookService, JwtService}
import com.wanari.tutelar.util.{DateTimeUtilCounterImpl, IdGeneratorCounterImpl}
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{verify, when}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class AuthServiceSpec extends TestBase {
  import cats.instances.try_._

  val authType             = "AUTH_TYPE"
  val externalId           = "ext_id"
  val customData           = "customData"
  val savedUser            = User("99999", 98765)
  val savedAccount         = Account(authType, "saved_ext_id", savedUser.id, "somedata")
  val providedData         = JsObject("userdata" -> JsString("helo"))
  val hookResponseLogin    = JsObject("group" -> JsString("log"))
  val hookResponseRegister = JsObject("group" -> JsString("reg"))

  trait TestScope {

    implicit val databaseService = new MemoryDatabaseService[Try]
    implicit val idGenerator     = new IdGeneratorCounterImpl[Try]
    implicit val timeService     = new DateTimeUtilCounterImpl[Try]
    implicit val jwtService      = mock[JwtService[Try]]
    implicit val hookService     = mock[HookService[Try]]

    databaseService.saveUser(savedUser)
    databaseService.saveAccount(savedAccount)

    when(jwtService.encode(any[JsObject], any[Option[Duration]])).thenReturn(Success("JWT"))
    when(hookService.register(any[String], any[String], any[String], any[JsObject]))
      .thenReturn(Success(hookResponseRegister))
    when(hookService.login(any[String], any[String], any[String], any[JsObject])).thenReturn(Success(hookResponseLogin))
    when(hookService.delete(any[String])).thenReturn(Success(()))

    val service = new AuthServiceImpl[Try]()
  }

  "#registerOrLogin" when {
    "register" should {
      "return the token" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData) shouldEqual Success("JWT")
      }
      "create token with the userid and hook response" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData).get
        verify(jwtService).encode(JsObject("id" -> JsString("1"), "group" -> JsString("reg")))
      }
      "create new user" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData).get
        databaseService.users("1") shouldEqual User("1", 1)
      }
      "save the account" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData).get
        databaseService.accounts((authType, externalId)) shouldEqual Account(authType, externalId, "1", customData)
      }
      "call hook service" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData).get
        verify(hookService).register("1", externalId, authType, providedData)
      }
    }
    "login" should {
      "return the token" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData) shouldEqual Success(
          "JWT"
        )
      }
      "create token with the userid and hook response" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData).get
        verify(jwtService).encode(JsObject("id" -> JsString(savedUser.id), "group" -> JsString("log")))
      }
      "update account custom data" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData).get
        databaseService.accounts((savedAccount.authType, savedAccount.externalId)) shouldEqual Account(
          savedAccount.authType,
          savedAccount.externalId,
          savedAccount.userId,
          customData
        )
      }
      "call hook service" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData).get
        verify(hookService).login(savedAccount.userId, savedAccount.externalId, authType, providedData)
      }
    }
    "the externalId" should {
      "be standardized and lowercased" in new TestScope {
        val externalId  = "SPEC_EXT_ID_\u0065\u0301"
        val standarized = "spec_ext_id_\u00e9"
        service.registerOrLogin(authType, externalId, customData, providedData) shouldEqual Success("JWT")
        databaseService.users("1") shouldEqual User("1", 1)
        verify(hookService).register("1", standarized, authType, providedData)
      }
    }
  }

  "#findCustomData" when {
    "user found" in new TestScope {
      service.findCustomData(savedAccount.authType, savedAccount.externalId).value shouldEqual Success(
        Some(
          savedAccount.customData
        )
      )
    }
    "the externalId should be standardized and lowercased" in new TestScope {
      val externalId  = "SPEC_EXT_ID_\u0065\u0301"
      val standarized = "spec_ext_id_\u00e9"

      val savedUser    = User("88888", 98765)
      val savedAccount = Account("AUTH_TYPE_S", standarized, savedUser.id, "customData")

      databaseService.saveUser(savedUser).get
      databaseService.saveAccount(savedAccount).get

      service.findCustomData(savedAccount.authType, externalId).value shouldEqual Success(
        Some(
          savedAccount.customData
        )
      )
    }
    "user not found" in new TestScope {
      service.findCustomData(authType, externalId).value shouldEqual Success(None)
    }
  }

  "#deleteUser" when {
    "user exists" should {
      "delete accounts" in new TestScope {
        service.deleteUser(savedUser.id).get
        databaseService.accounts.find(_._2.userId == savedUser.id) shouldEqual None
      }
      "delete user" in new TestScope {
        service.deleteUser(savedUser.id).get
        databaseService.users.get(savedUser.id) shouldEqual None
      }
      "call hook service" in new TestScope {
        service.deleteUser(savedUser.id).get
        verify(hookService).delete(savedUser.id)
      }
    }
    "user not found" in new TestScope {
      service.deleteUser("randomUserId") shouldBe a[Failure[_]]
    }
  }

  "#findUserIdInToken" when {
    "good token" in new TestScope {
      when(jwtService.validateAndDecode(any[String])).thenReturn(Success(JsObject("id" -> JsString("ID"))))
      service.findUserIdInToken("TOKEN").value shouldEqual Success(Some("ID"))
    }
    "wrong token if not contains id" in new TestScope {
      when(jwtService.validateAndDecode(any[String])).thenReturn(Success(JsObject("notid" -> JsString("ID"))))
      service.findUserIdInToken("TOKEN").value shouldBe Success(None)
    }
  }

}
