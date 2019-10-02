package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.core.AmqpService
import com.wanari.tutelar.core.AmqpService.AmqpQueueConfig
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.EmailServiceHttpConfig
import com.wanari.tutelar.util.HttpWrapper

object EmailServiceFactory {
  import DatabaseConfig._

  def create[F[_]: MonadError[*[_], Throwable]]()(
      implicit config: EmailServiceFactoryConfig,
      http: HttpWrapper[F],
      httpConfigF: EmailServiceHttpConfig,
      configByNameF: String => AmqpQueueConfig,
      amqpService: AmqpService[F]
  ): EmailService[F] = {
    config.`type` match {
      case HTTP => new EmailServiceHttpImpl[F]()
      case AMQP => new EmailServiceAmqpImpl[F]()
      case _    => throw WrongConfig(s"Unsupported email service type: ${config.`type`}")
    }
  }

  case class EmailServiceFactoryConfig(`type`: String)
  object DatabaseConfig {
    val HTTP = "http"
    val AMQP = "amqp"
  }
}
