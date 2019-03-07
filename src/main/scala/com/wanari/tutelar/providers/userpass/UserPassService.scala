package com.wanari.tutelar.providers.userpass
import com.wanari.tutelar.core.AuthService.Token

trait UserPassService[F[_]] {
  def login(username: String, password: String): F[Token]
}
