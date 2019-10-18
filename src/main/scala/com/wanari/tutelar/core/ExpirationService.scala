package com.wanari.tutelar.core

import com.wanari.tutelar.util.LoggerUtil.LogContext

import scala.concurrent.duration.Duration

trait ExpirationService[F[_]] {
  def isExpired(providerName: String, lastActivityAt: Long, loginAt: Long)(implicit ctx: LogContext): F[Boolean]
}

object ExpirationService {
  sealed trait ExpirationConfig
  case object ExpirationDisabled                      extends ExpirationConfig
  case class ExpirationInactivity(duration: Duration) extends ExpirationConfig
  case class ExpirationLifetime(duration: Duration)   extends ExpirationConfig
}
