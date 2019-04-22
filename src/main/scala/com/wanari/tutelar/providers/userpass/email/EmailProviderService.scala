package com.wanari.tutelar.providers.userpass.email

import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.providers.userpass.UserPassService
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait EmailProviderService[F[_]] extends UserPassService[F] {
  def register(registerToken: String, password: String, data: Option[JsObject])(implicit ctx: LogContext): F[Token]
  def sendRegister(email: String)(implicit ctx: LogContext): F[Unit]
  def resetPassword(resetPasswordToken: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): F[Token]
  def sendResetPassword(email: String)(implicit ctx: LogContext): F[Unit]
}

object EmailProviderService {
  case class EmailProviderConfig(
      url: String,
      username: String,
      password: String
  )
}
