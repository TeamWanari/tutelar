package com.wanari.tutelar.core.impl

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.impl.database.MemoryDatabaseService
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{HookService, JwtService}
import com.wanari.tutelar.util.LoggerUtil.LogContext
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

    implicit val databaseService           = new MemoryDatabaseService[Try]
    implicit val idGenerator               = new IdGeneratorCounterImpl[Try]
    implicit val timeService               = new DateTimeUtilCounterImpl[Try]
    implicit val longTermTokenServiceMock  = mock[JwtService[Try]]
    implicit val shortTermTokenServiceMock = mock[JwtService[Try]]
    implicit val hookService               = mock[HookService[Try]]

    databaseService.saveUser(savedUser)
    databaseService.saveAccount(savedAccount)

    when(longTermTokenServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn(Success("JWT_LONG"))
    when(shortTermTokenServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn(Success("JWT"))
    when(hookService.register(any[String], any[String], any[String], any[JsObject])(any[LogContext]))
      .thenReturn(Success(hookResponseRegister))
    when(hookService.login(any[String], any[String], any[String], any[JsObject])(any[LogContext]))
      .thenReturn(Success(hookResponseLogin))
    when(hookService.delete(any[String])(any[LogContext])).thenReturn(Success(()))
    when(hookService.link(any[String], any[String], any[String], any[JsObject])(any[LogContext]))
      .thenReturn(Success(hookResponseLogin))
    when(hookService.unlink(any[String], any[String], any[String])(any[LogContext])).thenReturn(Success(()))

    implicit def dummyConfigFunction(name: String): JwtConfig = null

    val service = new AuthServiceImpl[Try]() {
      override protected val longTermTokenService: JwtService[Try]  = longTermTokenServiceMock
      override protected val shortTermTokenService: JwtService[Try] = shortTermTokenServiceMock
    }
  }

  "#registerOrLogin" when {
    "register" should {
      "return the token" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData) shouldEqual Success(
          TokenData("JWT", "JWT_LONG")
        )
      }
      "create tokens with the userid and hook response" in new TestScope {
        service.registerOrLogin(authType, externalId, customData, providedData).get
        verify(longTermTokenServiceMock).encode(JsObject("id"  -> JsString("1"), "group" -> JsString("reg")))
        verify(shortTermTokenServiceMock).encode(JsObject("id" -> JsString("1"), "group" -> JsString("reg")))
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
        verify(hookService).register(eqTo("1"), eqTo(externalId), eqTo(authType), eqTo(providedData))(any[LogContext])
      }
    }
    "login" should {
      "return the token" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData) shouldEqual Success(
          TokenData("JWT", "JWT_LONG")
        )
      }
      "create tokens with the userid and hook response" in new TestScope {
        service.registerOrLogin(savedAccount.authType, savedAccount.externalId, customData, providedData).get
        verify(longTermTokenServiceMock).encode(JsObject("id"  -> JsString(savedUser.id), "group" -> JsString("log")))
        verify(shortTermTokenServiceMock).encode(JsObject("id" -> JsString(savedUser.id), "group" -> JsString("log")))
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
        verify(hookService).login(
          eqTo(savedAccount.userId),
          eqTo(savedAccount.externalId),
          eqTo(authType),
          eqTo(providedData)
        )(any[LogContext])
      }
    }
    "the externalId" should {
      "be standardized and lowercased" in new TestScope {
        val externalId  = "SPEC_EXT_ID_\u0065\u0301"
        val standarized = "spec_ext_id_\u00e9"
        service.registerOrLogin(authType, externalId, customData, providedData) shouldEqual Success(
          TokenData("JWT", "JWT_LONG")
        )
        databaseService.users("1") shouldEqual User("1", 1)
        verify(hookService).register(eqTo("1"), eqTo(standarized), eqTo(authType), eqTo(providedData))(any[LogContext])
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
        verify(hookService).delete(eqTo(savedUser.id))(any[LogContext])
      }
    }
    "user not found" in new TestScope {
      service.deleteUser("randomUserId") shouldBe a[Failure[_]]
    }
  }

  "#findUserIdInShortTermToken" when {
    "good token" in new TestScope {
      when(shortTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(Success(JsObject("id" -> JsString("ID"))))
      service.findUserIdInShortTermToken("TOKEN").value shouldEqual Success(Some("ID"))
    }
    "wrong token if not contains id" in new TestScope {
      when(shortTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(Success(JsObject("notid" -> JsString("ID"))))
      service.findUserIdInShortTermToken("TOKEN").value shouldBe Success(None)
    }
  }

  "#link" should {
    "fail if account is already used " in new TestScope {
      val savedUser2    = User("88888", 77777)
      val savedAccount2 = Account("type2", "some_ext_id", savedUser2.id, "somedata")
      databaseService.saveUser(savedUser2)
      databaseService.saveAccount(savedAccount2)
      service.link(savedUser2.id, savedAccount.authType, savedAccount.externalId, "", JsObject()) shouldBe a[Failure[_]]
    }
    "fail if user not found" in new TestScope {
      val account = Account("type2", "some_ext_id", savedUser.id, "somedata")
      service.link("missing_user", account.authType, account.externalId, "", JsObject()) shouldBe a[Failure[_]]
    }
    "fail if user already have this account type" in new TestScope {
      val account = Account(savedAccount.authType, "some_ext_id", savedUser.id, "somedata")
      service.link(savedUser.id, account.authType, account.externalId, "", JsObject()) shouldBe a[Failure[_]]
    }
    "save the account" in new TestScope {
      val account = Account("new_type", "ext_id", savedUser.id, "customData")
      service.link(savedUser.id, account.authType, account.externalId, account.customData, JsObject()).get
      databaseService.listAccountsByUserId(savedUser.id) shouldEqual Success(Seq(savedAccount, account))
    }
    "call hook service" in new TestScope {
      val account = Account("new_type", "ext_id", savedUser.id, "customData")
      service.link(savedUser.id, account.authType, account.externalId, account.customData, providedData).get
      verify(hookService).link(
        eqTo(savedAccount.userId),
        eqTo(account.externalId),
        eqTo(account.authType),
        eqTo(providedData)
      )(any[LogContext])
    }
  }

  "#unlink" when {
    "fail if it is the user's last account " in new TestScope {
      service.unlink(savedUser.id, savedAccount.authType) shouldBe a[Failure[_]]
    }
    "fail if account not found " in new TestScope {
      service.unlink(savedUser.id, "random_type") shouldBe a[Failure[_]]
    }
    "delete the account " in new TestScope {
      val account = Account("new_type", "some_ext_id", savedUser.id, "somedata")
      databaseService.saveAccount(account)
      service.unlink(savedUser.id, account.authType) shouldBe Success(())
      databaseService.listAccountsByUserId(savedUser.id) shouldEqual Success(Seq(savedAccount))
    }
    "call hook servicet " in new TestScope {
      val account = Account("new_type", "some_ext_id", savedUser.id, "somedata")
      databaseService.saveAccount(account)
      service.unlink(savedUser.id, account.authType) shouldBe Success(())
      verify(hookService).unlink(eqTo(savedUser.id), eqTo(account.externalId), eqTo(account.authType))(any[LogContext])
    }
  }

}
