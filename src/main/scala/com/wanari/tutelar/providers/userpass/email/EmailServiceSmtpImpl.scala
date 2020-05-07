package com.wanari.tutelar.providers.userpass.email

import cats.Applicative
import com.wanari.tutelar.providers.userpass.email.EmailServiceSmtpImpl.EmailServiceSmtpConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import org.apache.commons.mail.{DefaultAuthenticator, HtmlEmail}

class EmailServiceSmtpImpl[F[_]: Applicative](implicit
    config: EmailServiceSmtpConfig
) extends EmailService[F] {
  import cats.syntax.applicative._
  override def sendRegisterUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    val title = config.template.registerTitle
    val body  = config.template.registerBody.replace("<<TOKEN>>", token)
    send(email, title, body)
  }

  override def sendResetPasswordUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    val title = config.template.resetPasswordTitle
    val body  = config.template.resetPasswordBody.replace("<<TOKEN>>", token)
    send(email, title, body)
  }

  private def send(address: String, title: String, body: String): F[Unit] = {
    val sender = config.template.senderAddress
    val email  = createNewEmail()
    email.setFrom(sender)
    email.addTo(address)
    email.setSubject(title)
    email.setHtmlMsg(body)
    email.send()
    ().pure[F]
  }

  private def createNewEmail(): HtmlEmail = {
    val smtpConfig = config.server
    val email      = new HtmlEmail
    email.setHostName(smtpConfig.host)
    if (!smtpConfig.username.isEmpty) {
      email.setAuthenticator(new DefaultAuthenticator(smtpConfig.username, smtpConfig.password))
    }
    email.setSSLOnConnect(smtpConfig.ssl)
    if (smtpConfig.ssl) {
      email.setSslSmtpPort(smtpConfig.port.toString)
    } else {
      email.setSmtpPort(smtpConfig.port)
    }
    email
  }
}

object EmailServiceSmtpImpl {
  case class SmtpConfig(
      host: String,
      port: Int,
      ssl: Boolean,
      username: String,
      password: String
  )
  case class EmailTemplateConfig(
      senderAddress: String,
      registerTitle: String,
      registerBody: String,
      resetPasswordTitle: String,
      resetPasswordBody: String
  )
  case class EmailServiceSmtpConfig(
      server: SmtpConfig,
      template: EmailTemplateConfig
  )
}
