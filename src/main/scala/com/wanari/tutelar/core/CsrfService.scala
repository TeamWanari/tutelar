package com.wanari.tutelar.core

trait CsrfService[F[_]] {
  def getCsrfToken(auther: String): F[String]
  def checkCsrfToken(auther: String, str: String): F[Unit]
}
