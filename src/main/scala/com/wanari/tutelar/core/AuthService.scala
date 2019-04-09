package com.wanari.tutelar.core

import cats.data.OptionT
import com.wanari.tutelar.core.AuthService.Token
import spray.json.JsObject

trait AuthService[F[_]] {
  def findCustomData(authType: String, externalId: String): OptionT[F, String]
  def registerOrLogin(authType: String, externalId: String, customData: String, providedData: JsObject): F[Token]
  def deleteUser(userId: String): F[Unit]
  def findUserIdInToken(token: String): OptionT[F, String]
  def link(userId: String, authType: String, externalId: String, customData: String, providedData: JsObject): F[Unit]
  def unlink(userId: String, authType: String): F[Unit]
}

object AuthService {
  type Token = String
}
