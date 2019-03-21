package com.wanari.tutelar.providers.userpass.email

trait EmailService[F[_]] {
  def sendRegisterUrl(email: String, url: String): F[Unit]
}
