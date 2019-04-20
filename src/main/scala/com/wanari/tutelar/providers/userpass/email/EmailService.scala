package com.wanari.tutelar.providers.userpass.email

import com.wanari.tutelar.util.LoggerUtil.LogContext

trait EmailService[F[_]] {
  def sendRegisterUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit]
  def sendResetPasswordUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit]
}
