package com.wanari.tutelar.providers.userpass
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait UserPassService[F[_]] {
  def login(username: String, password: String, data: Option[JsObject], refreshToken: Option[LongTermToken])(
      implicit ctx: LogContext
  ): ErrorOr[F, TokenData]
}
