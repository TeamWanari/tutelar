package com.wanari.tutelar.providers.userpass.email

import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.providers.userpass.UserPassService
import spray.json.JsObject

trait EmailProviderService[F[_]] extends UserPassService[F] {
  def register(registerToken: String, password: String, data: Option[JsObject]): F[Token]
  def sendRegister(email: String): F[Unit]
}

object EmailProviderService {
  case class EmailProviderConfig(url: String, username: String, password: String, registerUrl: String)
}
