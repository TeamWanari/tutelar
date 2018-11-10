package com.wanari.tutelar.core

import com.wanari.tutelar.core.AuthService.CallbackUrl

trait AuthService[F[_]] {
  def registerOrLogin(authType: String, externalId: String, customData: String): F[CallbackUrl]
}

object AuthService {
  type CallbackUrl = String

  case class AuthConfig(callback: CallbackUrl)
}
