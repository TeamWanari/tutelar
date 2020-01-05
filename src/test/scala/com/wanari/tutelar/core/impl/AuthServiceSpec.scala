package com.wanari.tutelar.core.impl

import cats.data.{EitherT, OptionT}
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.Errors._
import com.wanari.tutelar.core.impl.database.MemoryDatabaseService
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{ExpirationService, HookService, JwtService}
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{DateTimeUtilCounterImpl, IdGeneratorCounterImpl}
import org.mockito.ArgumentMatchersSugar._
import spray.json.{JsArray, JsNumber, JsObject, JsString}

import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

class AuthServiceSpec extends TestBase {
  import cats.instances.try_._

  val authType             = "AUTH_TYPE"
  val externalId           = "ext_id"
  val customData           = "customData"
  val savedUser            = User("99999", 98765)
  val savedAccount         = Account("AUTH_TYPE_SAVED", "saved_ext_id", savedUser.id, "somedata")
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

    def expirationServiceIsExpire(providerName: String) = false
    implicit val expirationService = new ExpirationService[Try] {
      override def isExpired(providerName: String, lastActivityAt: Long, loginAt: Long)(
          implicit ctx: LogContext
      ): Try[Boolean] = Success(expirationServiceIsExpire(providerName))
    }

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

  "#authenticatedWith" when {
    "register" should {
      "return the token" in new TestScope {
        service.authenticatedWith(authType, externalId, customData, providedData, None) shouldEqual EitherT.rightT(
          TokenData("JWT", "JWT_LONG")
        )
      }
      "create tokens with the userid and hook response" in new TestScope {
        service.authenticatedWith(authType, externalId, customData, providedData, None)
        verify(longTermTokenServiceMock).encode(
          JsObject(
            "id" -> JsString("1"),
            "providers" -> JsArray(
              JsObject(
                "name"    -> JsString(authType),
                "loginAt" -> JsNumber(timeService.counter.get())
              )
            ),
            "data"      -> JsObject("group" -> JsString("reg")),
            "createdAt" -> JsNumber(timeService.counter.get())
          )
        )
        verify(shortTermTokenServiceMock).encode(
          JsObject(
            "id"    -> JsString("1"),
            "group" -> JsString("reg"),
            "providers" -> JsArray(
              JsString(authType)
            )
          )
        )
      }
      "link to account" in new TestScope {
        val longTermTokenData = JsObject(
          "id" -> JsString(savedUser.id),
          "providers" -> JsArray(
            JsObject(
              "name"    -> JsString(savedAccount.authType),
              "loginAt" -> JsNumber(678)
            )
          ),
          "data"      -> JsObject("group" -> JsString("old_group_data")),
          "createdAt" -> JsNumber(777)
        )
        when(longTermTokenServiceMock.validateAndDecode(any[String]))
          .thenReturn(EitherT.right(Success(longTermTokenData)))

        val longTermToken = "long_term_token"

        service.authenticatedWith(authType, externalId, customData, providedData, Option(longTermToken))
        verify(longTermTokenServiceMock).validateAndDecode(longTermToken)
        verify(longTermTokenServiceMock).encode(
          JsObject(
            "id" -> JsString(savedUser.id),
            "providers" -> JsArray(
              JsObject(
                "name"    -> JsString(savedAccount.authType),
                "loginAt" -> JsNumber(678)
              ),
              JsObject(
                "name"    -> JsString(authType),
                "loginAt" -> JsNumber(timeService.counter.get())
              )
            ),
            "data"      -> JsObject("group" -> JsString("log")),
            "createdAt" -> JsNumber(timeService.counter.get())
          )
        )
        verify(shortTermTokenServiceMock).encode(
          JsObject(
            "id"    -> JsString(savedUser.id),
            "group" -> JsString("log"),
            "providers" -> JsArray(
              JsString(savedAccount.authType),
              JsString(authType)
            )
          )
        )
      }
      "create new user" in new TestScope {
        service.authenticatedWith(authType, externalId, customData, providedData, None)
        databaseService.users("1") shouldEqual User("1", 1)
      }
      "save the account" in new TestScope {
        service.authenticatedWith(authType, externalId, customData, providedData, None)
        databaseService.accounts((authType, externalId)) shouldEqual Account(authType, externalId, "1", customData)
      }
      "call hook service" in new TestScope {
        service.authenticatedWith(authType, externalId, customData, providedData, None)
        verify(hookService).register(eqTo("1"), eqTo(externalId), eqTo(authType), eqTo(providedData))(any[LogContext])
      }
    }
    "login" should {
      "return the token" in new TestScope {
        service.authenticatedWith(savedAccount.authType, savedAccount.externalId, customData, providedData, None) shouldEqual EitherT
          .rightT(
            TokenData("JWT", "JWT_LONG")
          )
      }
      "create tokens with the userid and hook response" in new TestScope {
        service.authenticatedWith(savedAccount.authType, savedAccount.externalId, customData, providedData, None)
        verify(longTermTokenServiceMock).encode(
          JsObject(
            "id" -> JsString(savedUser.id),
            "providers" -> JsArray(
              JsObject(
                "name"    -> JsString(savedAccount.authType),
                "loginAt" -> JsNumber(timeService.counter.get())
              )
            ),
            "data"      -> JsObject("group" -> JsString("log")),
            "createdAt" -> JsNumber(timeService.counter.get())
          )
        )
        verify(shortTermTokenServiceMock).encode(
          JsObject(
            "id"    -> JsString(savedUser.id),
            "group" -> JsString("log"),
            "providers" -> JsArray(
              JsString(savedAccount.authType)
            )
          )
        )
      }
      "update account custom data" in new TestScope {
        service.authenticatedWith(savedAccount.authType, savedAccount.externalId, customData, providedData, None)
        databaseService.accounts((savedAccount.authType, savedAccount.externalId)) shouldEqual Account(
          savedAccount.authType,
          savedAccount.externalId,
          savedAccount.userId,
          customData
        )
      }
      "call hook service" in new TestScope {
        service.authenticatedWith(savedAccount.authType, savedAccount.externalId, customData, providedData, None)
        verify(hookService).login(
          eqTo(savedAccount.userId),
          eqTo(savedAccount.externalId),
          eqTo(savedAccount.authType),
          eqTo(providedData)
        )(any[LogContext])
      }
    }
    "the externalId" should {
      "be standardized and lowercased" in new TestScope {
        val externalId  = "SPEC_EXT_ID_\u0065\u0301"
        val standarized = "spec_ext_id_\u00e9"
        service.authenticatedWith(authType, externalId, customData, providedData, None) shouldEqual EitherT.rightT(
          TokenData("JWT", "JWT_LONG")
        )
        databaseService.users("1") shouldEqual User("1", 1)
        verify(hookService).register(eqTo("1"), eqTo(standarized), eqTo(authType), eqTo(providedData))(any[LogContext])
      }
    }
  }

  "#findCustomData" when {
    "user found" in new TestScope {
      service.findCustomData(savedAccount.authType, savedAccount.externalId) shouldEqual OptionT.some(
        savedAccount.customData
      )
    }
    "the externalId should be standardized and lowercased" in new TestScope {
      val externalId  = "SPEC_EXT_ID_\u0065\u0301"
      val standarized = "spec_ext_id_\u00e9"

      val savedUser    = User("88888", 98765)
      val savedAccount = Account("AUTH_TYPE_S", standarized, savedUser.id, "customData")

      databaseService.saveUser(savedUser)
      databaseService.saveAccount(savedAccount)

      service.findCustomData(savedAccount.authType, externalId) shouldEqual OptionT.some(savedAccount.customData)
    }
    "user not found" in new TestScope {
      service.findCustomData(authType, externalId) shouldEqual OptionT.none
    }
  }

  "#deleteUser" when {
    "user exists" should {
      "delete accounts" in new TestScope {
        service.deleteUser(savedUser.id)
        databaseService.accounts.find(_._2.userId == savedUser.id) shouldEqual None
      }
      "delete user" in new TestScope {
        service.deleteUser(savedUser.id)
        databaseService.users.get(savedUser.id) shouldEqual None
      }
      "call hook service" in new TestScope {
        service.deleteUser(savedUser.id)
        verify(hookService).delete(eqTo(savedUser.id))(any[LogContext])
      }
    }
    "user not found" in new TestScope {
      service.deleteUser("randomUserId") shouldBe EitherT.leftT(UserNotFound())
    }
  }

  "#findUserIdInShortTermToken" when {
    "good token" in new TestScope {
      when(shortTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(JsObject("id" -> JsString("ID")))))
      service.findUserIdInShortTermToken("TOKEN") shouldEqual OptionT.some("ID")
    }
    "wrong token if not contains id" in new TestScope {
      when(shortTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(JsObject("notid" -> JsString("ID")))))
      service.findUserIdInShortTermToken("TOKEN") shouldBe OptionT.none
    }
  }

  "#link" should {
    "fail if account is already used " in new TestScope {
      val savedUser2    = User("88888", 77777)
      val savedAccount2 = Account("type2", "some_ext_id", savedUser2.id, "somedata")
      databaseService.saveUser(savedUser2)
      databaseService.saveAccount(savedAccount2)
      service
        .link(savedUser2.id, savedAccount.authType, savedAccount.externalId, "", JsObject()) shouldBe EitherT.leftT(
        AccountUsed()
      )
    }
    "fail if user not found" in new TestScope {
      val account = Account("type2", "some_ext_id", savedUser.id, "somedata")
      service.link("missing_user", account.authType, account.externalId, "", JsObject()) shouldBe EitherT.leftT(
        UserNotFound()
      )
    }
    "fail if user already have this account type" in new TestScope {
      val account = Account(savedAccount.authType, "some_ext_id", savedUser.id, "somedata")
      service.link(savedUser.id, account.authType, account.externalId, "", JsObject()) shouldBe EitherT.leftT(
        UserHadThisAccountType()
      )
    }
    "save the account" in new TestScope {
      val account = Account("new_type", "ext_id", savedUser.id, "customData")
      service.link(savedUser.id, account.authType, account.externalId, account.customData, JsObject())
      databaseService.listAccountsByUserId(savedUser.id) shouldEqual Success(Seq(savedAccount, account))
    }
    "call hook service" in new TestScope {
      val account = Account("new_type", "ext_id", savedUser.id, "customData")
      service.link(savedUser.id, account.authType, account.externalId, account.customData, providedData)
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
      service.unlink(savedUser.id, savedAccount.authType) shouldBe EitherT.leftT(UserLastAccount())
    }
    "fail if account not found " in new TestScope {
      val savedAccount2 = Account("AUTH_TYPE2", "saved_ext_id", savedUser.id, "somedata")
      databaseService.saveAccount(savedAccount2)
      service.unlink(savedUser.id, "random_type") shouldBe EitherT.leftT(AccountNotFound())
    }
    "delete the account " in new TestScope {
      val account = Account("new_type", "some_ext_id", savedUser.id, "somedata")
      databaseService.saveAccount(account)
      service.unlink(savedUser.id, account.authType) shouldBe EitherT.rightT(())
      databaseService.listAccountsByUserId(savedUser.id) shouldEqual Success(Seq(savedAccount))
    }
    "call hook service " in new TestScope {
      val account = Account("new_type", "some_ext_id", savedUser.id, "somedata")
      databaseService.saveAccount(account)
      service.unlink(savedUser.id, account.authType) shouldBe EitherT.rightT(())
      verify(hookService).unlink(eqTo(savedUser.id), eqTo(account.externalId), eqTo(account.authType))(any[LogContext])
    }
  }

  "#refresh-token" when {
    import com.wanari.tutelar.util.SpraySyntax._

    val originalTokenData =
      JsObject(
        "id" -> JsString(savedUser.id),
        "providers" -> JsArray(
          JsObject(
            "name"    -> JsString(savedAccount.authType),
            "loginAt" -> JsNumber(888)
          ),
          JsObject(
            "name"    -> JsString("EXPIRED_PROVIDER"),
            "loginAt" -> JsNumber(1000)
          )
        ),
        "data"      -> JsObject("randomData" -> JsString("data")),
        "createdAt" -> JsNumber(999)
      )

    "validate as long term token" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(JsObject("id" -> JsString(savedUser.id)))))
      service.refreshToken("long_term_token")
      verify(longTermTokenServiceMock).validateAndDecode("long_term_token")
    }
    "create new token with the previous token data refresh the createdAt in longterm token" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData)))
      when(hookService.refreshToken(any[String])(any[LogContext]))
        .thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token")
      val expectedRefreshToken = originalTokenData + ("createdAt" -> JsNumber(timeService.counter.get()))
      val expectedShortToken = JsObject(
        "id"         -> JsString(savedUser.id),
        "randomData" -> JsString("data"),
        "providers" -> JsArray(
          JsString(savedAccount.authType),
          JsString("EXPIRED_PROVIDER")
        )
      )
      verify(longTermTokenServiceMock).encode(expectedRefreshToken)
      verify(shortTermTokenServiceMock).encode(expectedShortToken)
    }
    "create new token data from hook" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData)))
      when(hookService.refreshToken(any[String])(any[LogContext]))
        .thenReturn(Success(JsObject("new" -> JsString("data"))))
      service.refreshToken("long_term_token")
      val expectedRefreshToken = originalTokenData +
        ("createdAt" -> JsNumber(timeService.counter.get())) +
        ("data"      -> JsObject("new" -> JsString("data")))
      val expectedShortToken = JsObject(
        "id"  -> JsString(savedUser.id),
        "new" -> JsString("data"),
        "providers" -> JsArray(
          JsString(savedAccount.authType),
          JsString("EXPIRED_PROVIDER")
        )
      )
      verify(longTermTokenServiceMock).encode(expectedRefreshToken)
      verify(shortTermTokenServiceMock).encode(expectedShortToken)
    }
    "remove expired providers" in new TestScope {
      override def expirationServiceIsExpire(providerName: String): Boolean = providerName == "EXPIRED_PROVIDER"
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData)))
      when(hookService.refreshToken(any[String])(any[LogContext]))
        .thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token")
      val expectedRefreshToken = JsObject(
        "id" -> JsString(savedUser.id),
        "providers" -> JsArray(
          JsObject(
            "name"    -> JsString(savedAccount.authType),
            "loginAt" -> JsNumber(888)
          )
        ),
        "data"      -> JsObject("randomData" -> JsString("data")),
        "createdAt" -> JsNumber(timeService.counter.get())
      )
      val expectedShortToken = JsObject(
        "id"         -> JsString(savedUser.id),
        "randomData" -> JsString("data"),
        "providers" -> JsArray(
          JsString(savedAccount.authType)
        )
      )
      verify(longTermTokenServiceMock).encode(expectedRefreshToken)
      verify(shortTermTokenServiceMock).encode(expectedShortToken)
    }
    "return with new token data" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData)))
      when(hookService.refreshToken(any[String])(any[LogContext])).thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token") shouldBe EitherT.rightT(TokenData("JWT", "JWT_LONG"))
    }
    "fail if invalid data" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(JsObject("notid" -> JsString(savedUser.id)))))
      when(hookService.refreshToken(any[String])(any[LogContext])).thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token") shouldBe EitherT.leftT(InvalidToken())
    }
    "fail if user not found" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData + ("id" -> JsString("random_id")))))
      when(hookService.refreshToken(any[String])(any[LogContext])).thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token") shouldBe EitherT.leftT(UserNotFound())
    }
    "fail if validate failed" in new TestScope {
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.left(Success(InvalidJwt())))
      when(hookService.refreshToken(any[String])(any[LogContext])).thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token") shouldBe EitherT.leftT(InvalidJwt())
    }
    "fail when expire all provider login" in new TestScope {
      override def expirationServiceIsExpire(providerName: String): Boolean = true
      when(longTermTokenServiceMock.validateAndDecode(any[String]))
        .thenReturn(EitherT.right(Success(originalTokenData)))
      when(hookService.refreshToken(any[String])(any[LogContext])).thenReturn(Success(JsObject.empty))
      service.refreshToken("long_term_token") shouldBe EitherT.leftT(LoginExpired())
    }
  }
}
