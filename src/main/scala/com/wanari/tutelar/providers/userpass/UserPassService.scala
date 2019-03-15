package com.wanari.tutelar.providers.userpass
import com.wanari.tutelar.core.AuthService.Token
import spray.json.JsObject

trait UserPassService[F[_]] {
  def login(username: String, password: String, data: Option[JsObject]): F[Token]
  def register(username: String, password: String, data: Option[JsObject]): F[Token] = ???
}
