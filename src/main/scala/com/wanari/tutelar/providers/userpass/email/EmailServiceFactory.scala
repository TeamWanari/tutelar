package com.wanari.tutelar.providers.userpass.email

import cats.MonadError
import com.wanari.tutelar.core.AmqpService
import com.wanari.tutelar.core.AmqpService.AmqpQueueConfig
import com.wanari.tutelar.core.Errors.WrongConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceSmtpImpl.EmailServiceSmtpConfig

object EmailServiceFactory {
  import DatabaseConfig._

  def create[F[_]: MonadError[*[_], Throwable]]()(implicit
      config: EmailServiceFactoryConfig,
      smtpEmailCongif: EmailServiceSmtpConfig,
      configByNameF: String => AmqpQueueConfig,
      amqpService: AmqpService[F]
  ): EmailService[F] = {
    config.`type` match {
      case AMQP => new EmailServiceAmqpImpl[F]()
      case SMTP => new EmailServiceSmtpImpl[F]()
      case _    => throw WrongConfig(s"Unsupported email service type: ${config.`type`}")
    }
  }

  case class EmailServiceFactoryConfig(`type`: String)
  object DatabaseConfig {
    val AMQP = "amqp"
    val SMTP = "smtp"
  }
}
