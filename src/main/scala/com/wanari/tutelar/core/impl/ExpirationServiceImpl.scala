package com.wanari.tutelar.core.impl

import cats.Applicative
import com.wanari.tutelar.core.ExpirationService
import com.wanari.tutelar.core.ExpirationService._
import com.wanari.tutelar.util.DateTimeUtil
import com.wanari.tutelar.util.LoggerUtil.{LogContext, Logger}

import scala.concurrent.duration.Duration

class ExpirationServiceImpl[F[_]: Applicative](
    implicit providerExpirationConfigs: Map[String, ExpirationConfig],
    dateTime: DateTimeUtil[F]
) extends ExpirationService[F] {
  import cats.syntax.applicative._
  import cats.syntax.functor._

  private val logger = new Logger("ExpirationService")

  override def isExpired(providerName: String, lastActivityAt: Long, loginAt: Long)(
      implicit ctx: LogContext
  ): F[Boolean] = {
    lazy val notFoundConfigFallback = {
      logger.warn(s"Not found expiration config for: $providerName.")
      ExpirationDisabled
    }
    providerExpirationConfigs.getOrElse(providerName.toLowerCase, notFoundConfigFallback) match {
      case ExpirationDisabled             => false.pure[F]
      case ExpirationInactivity(duration) => isItOlderThan(lastActivityAt, duration)
      case ExpirationLifetime(duration)   => isItOlderThan(loginAt, duration)
    }
  }

  private def isItOlderThan(time: Long, duration: Duration): F[Boolean] = {
    dateTime.getCurrentTimeMillis.map { now => time + duration.toMillis <= now }
  }
}
