package com.wanari.tutelar.core

import cats.data.OptionT
import com.wanari.tutelar.core.AuthService.Token
import spray.json.JsObject

trait AuthService[F[_]] {
  def findCustomData(authType: String, externalId: String): OptionT[F, String]
  def registerOrLogin(authType: String, externalId: String, customData: String, providedData: JsObject): F[Token]
}

object AuthService {
  type Token = String
}
