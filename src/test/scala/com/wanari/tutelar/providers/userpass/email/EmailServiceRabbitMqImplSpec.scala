package com.wanari.tutelar.providers.userpass.email

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.RabbitMqServiceMock
import com.wanari.tutelar.providers.userpass.email.EmailServiceRabbitMqImpl.EmailServiceRabbitMqConfig
import spray.json._

import scala.util.{Success, Try}

class EmailServiceRabbitMqImplSpec extends TestBase {
  import cats.instances.try_._

  trait TestScope {
    implicit lazy val rabbitMqMock: RabbitMqServiceMock[Try] = new RabbitMqServiceMock[Try]()
    implicit lazy val config: () => Try[EmailServiceRabbitMqConfig] = () => {
      Success(EmailServiceRabbitMqConfig("QUEUE"))
    }
    lazy val service = new EmailServiceRabbitMqImpl[Try]()
  }

  "EmailServiceRabbitMqImpl" should {
    "sendRegister" in new TestScope {
      service.sendRegisterUrl("EMAIL", "TOKEN") shouldEqual Success(())
      rabbitMqMock.calls shouldEqual Seq("QUEUE" -> """{"type":"register","email":"EMAIL","token":"TOKEN"}""".parseJson)
    }
    "sendResetPasswordUrl" in new TestScope {
      service.sendResetPasswordUrl("EMAIL", "TOKEN") shouldEqual Success(())
      rabbitMqMock.calls shouldEqual Seq(
        "QUEUE" -> """{"type":"reset-password","email":"EMAIL","token":"TOKEN"}""".parseJson
      )
    }
  }
}
