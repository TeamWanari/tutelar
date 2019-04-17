package com.wanari.tutelar.providers.userpass.email

trait EmailService[F[_]] {
  def sendRegisterUrl(email: String, token: String): F[Unit]
  def sendResetPasswordUrl(email: String, token: String): F[Unit]
}
