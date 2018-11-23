package com.wanari.tutelar.core

import com.wanari.tutelar.core.AuthService.Token
import spray.json.JsObject

trait AuthService[F[_]] {
  def registerOrLogin(authType: String, externalId: String, customData: String, providedData: JsObject): F[Token]
}

object AuthService {
  type Token = String
}
