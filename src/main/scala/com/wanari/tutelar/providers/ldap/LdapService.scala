package com.wanari.tutelar.providers.ldap
import com.wanari.tutelar.core.AuthService.Token

trait LdapService[F[_]] {
  def login(username: String, password: String): F[Token]
}
