package com.wanari.tutelar.providers.userpass.email

import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.providers.userpass.UserPassService
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait EmailProviderService[F[_]] extends UserPassService[F] with Initable[F] {
  def register(registerToken: String, password: String, data: Option[JsObject], refreshToken: Option[LongTermToken])(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData]
  def sendRegister(email: String)(implicit ctx: LogContext): ErrorOr[F, Unit]
  def resetPassword(resetPasswordToken: String, password: String, data: Option[JsObject])(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData]
  def sendResetPassword(email: String)(implicit ctx: LogContext): ErrorOr[F, Unit]
}
