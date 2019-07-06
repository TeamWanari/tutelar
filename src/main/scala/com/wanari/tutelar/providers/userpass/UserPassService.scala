package com.wanari.tutelar.providers.userpass
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait UserPassService[F[_]] {
  def login(username: String, password: String, data: Option[JsObject])(implicit ctx: LogContext): F[TokenData]
}
