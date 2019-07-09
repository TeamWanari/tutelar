package com.wanari.tutelar.providers.userpass.email

import cats.Monad
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AmqpService
import com.wanari.tutelar.core.AmqpService.{AmqpQueue, AmqpQueueConfig}
import com.wanari.tutelar.providers.userpass.email.EmailServiceAmqpImpl.TokenMessage
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.RootJsonFormat

class EmailServiceAmqpImpl[F[_]: Monad](
    implicit amqpService: AmqpService[F],
    configByNameF: String => F[AmqpQueueConfig]
) extends EmailService[F]
    with Initable[F] {
  import cats.syntax.functor._

  protected lazy val queue: F[AmqpQueue] = {
    configByNameF("email_service").map(amqpService.createQueue)
  }

  override def init: F[Unit] = {
    queue.map(_ => ())
  }

  override def sendRegisterUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    queue.map(_.send(TokenMessage("register", email, token)))
  }

  override def sendResetPasswordUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    queue.map(_.send(TokenMessage("reset-password", email, token)))
  }
}

object EmailServiceAmqpImpl {
  import spray.json.DefaultJsonProtocol._
  private case class TokenMessage(`type`: String, email: String, token: String)
  private implicit val tokenMessageFormat: RootJsonFormat[TokenMessage] = jsonFormat3(TokenMessage)
}
