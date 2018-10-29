package com.wanari.tutelar.jwt

import com.wanari.tutelar.jwt.JwtConfigService.JwtConfig

import scala.concurrent.duration.Duration

trait JwtConfigService[F[_]] {
  def getConfig: F[JwtConfig]
}

object JwtConfigService {
  case class JwtConfig(
      expirationTime: Duration,
      algorithm: String,
      secret: String,
      privateKey: String,
      publicKey: String
  )
}
