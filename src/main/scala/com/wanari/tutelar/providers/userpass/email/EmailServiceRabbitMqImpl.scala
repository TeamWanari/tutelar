package com.wanari.tutelar.providers.userpass.email

import cats.Monad
import com.wanari.tutelar.core.RabbitMqService
import com.wanari.tutelar.providers.userpass.email.EmailServiceRabbitMqImpl.EmailServiceRabbitMqConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json._

// TODO use it
class EmailServiceRabbitMqImpl[F[_]: Monad](
    implicit rabbitMq: RabbitMqService[F],
    configF: () => F[EmailServiceRabbitMqConfig]
) extends EmailService[F] {
  import EmailServiceRabbitMqImpl._
  import cats.syntax.functor._

  override def sendRegisterUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    send(TokenMessage("register", email, token))
  }

  override def sendResetPasswordUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    send(TokenMessage("reset-password", email, token))
  }

  private def send(msg: TokenMessage): F[Unit] = {
    configF().map { config =>
      rabbitMq.send(config.queue, msg)
    }
  }
}

object EmailServiceRabbitMqImpl {
  import spray.json.DefaultJsonProtocol._
  case class EmailServiceRabbitMqConfig(queue: String)
  private case class TokenMessage(`type`: String, email: String, token: String)
  private implicit val tokenMessageFormat: RootJsonFormat[TokenMessage] = jsonFormat3(TokenMessage)
}
