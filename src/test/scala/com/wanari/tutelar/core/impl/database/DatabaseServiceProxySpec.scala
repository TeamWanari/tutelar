package com.wanari.tutelar.core.impl.database

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.DatabaseService.{Account, AccountId, User}
import com.wanari.tutelar.core.impl.database.DatabaseServiceProxy.DatabaseServiceProxyConfig
import org.mockito.ArgumentMatchersSugar._
import org.mockito.Mockito.{reset, verify, when, never}
import org.scalatest.BeforeAndAfterEach

import scala.util.{Failure, Success, Try}

class DatabaseServiceProxySpec extends TestBase with BeforeAndAfterEach {
  import cats.instances.try_._

  val service1 = mock[DatabaseService[Try]]
  val service2 = mock[DatabaseService[Try]]

  override def beforeEach(): Unit = {
    resetMock(service1)
    resetMock(service2)
  }

  trait TestScope {
    val selectedDatabase: String
    lazy val databaseServices = Map[String, () => DatabaseService[Try]](
      "type1" -> (() => service1),
      "type2" -> (() => service2)
    )
    implicit lazy val config = () => Success(DatabaseServiceProxyConfig(selectedDatabase))
    lazy val service         = new DatabaseServiceProxy[Try](databaseServices)
  }

  "DatabaseServiceProxy" when {
    "#init" should {
      "return error if not supported database" in new TestScope {
        val selectedDatabase = "type55"
        service.init shouldBe a[Failure[_]]
      }
    }
    Seq(
      ("init", (s: DatabaseService[Try]) => s.init),
      ("checkStatus", (s: DatabaseService[Try]) => s.checkStatus()),
      ("saveUser", (s: DatabaseService[Try]) => s.saveUser(null)),
      ("saveAccount", (s: DatabaseService[Try]) => s.saveAccount(null)),
      ("findUserById", (s: DatabaseService[Try]) => s.findUserById(null)),
      ("findAccountByTypeAndExternalId", (s: DatabaseService[Try]) => s.findAccountByTypeAndExternalId(null)),
      ("listAccountsByUserId", (s: DatabaseService[Try]) => s.listAccountsByUserId(null)),
      ("updateCustomData", (s: DatabaseService[Try]) => s.updateCustomData(null, null)),
      ("deleteUserWithAccountsById", (s: DatabaseService[Try]) => s.deleteUserWithAccountsById(null)),
      ("deleteAccountByUserAndType", (s: DatabaseService[Try]) => s.deleteAccountByUserAndType(null, null))
    ).foreach {
      case (name, func) =>
        s"#$name" should {
          Seq(("type1", service1, service2), ("type2", service2, service1)).foreach {
            case (dbType, calledService, notCalledService) =>
              s"call the selected service's $name - $dbType" in new TestScope {
                override val selectedDatabase: String = dbType
                func(service) shouldBe a[Success[_]]
                func(verify(calledService))
                func(verify(notCalledService, never))
              }
          }
        }
    }
  }

  private def resetMock(service: DatabaseService[Try]) = {
    reset(service)
    when(service.init).thenReturn(Success(()))
    when(service.checkStatus()).thenReturn(Success(true))
    when(service.saveUser(any[User])).thenReturn(Success(()))
    when(service.saveAccount(any[Account])).thenReturn(Success(()))
    when(service.findUserById(any[String])).thenReturn(Success(None))
    when(service.findAccountByTypeAndExternalId(any[AccountId])).thenReturn(Success(None))
    when(service.listAccountsByUserId(any[String])).thenReturn(Success(Seq.empty))
    when(service.updateCustomData(any[AccountId], any[String])).thenReturn(Success(()))
    when(service.deleteUserWithAccountsById(any[String])).thenReturn(Success(()))
    when(service.deleteAccountByUserAndType(any[String], any[String])).thenReturn(Success(()))
  }
}
