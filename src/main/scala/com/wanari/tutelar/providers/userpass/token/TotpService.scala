package com.wanari.tutelar.providers.userpass.token

import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.providers.userpass.UserPassService
import com.wanari.tutelar.providers.userpass.token.TotpServiceImpl.QRData
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait TotpService[F[_]] extends UserPassService[F] with Initable[F] {
  def register(
      userName: String,
      registerToken: String,
      password: String,
      data: Option[JsObject],
      refreshToken: Option[LongTermToken]
  )(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData]
  def qrCodeData(implicit ctx: LogContext): ErrorOr[F, QRData]
}
