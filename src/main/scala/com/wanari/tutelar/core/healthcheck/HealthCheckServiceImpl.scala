package com.wanari.tutelar.core.healthcheck

import cats.MonadError
import com.wanari.tutelar.BuildInfo
import com.wanari.tutelar.core.config.ServerConfig
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]](
    implicit F: MonadError[F, Throwable],
    config: ServerConfig[F],
    databaseService: DatabaseService[F]
) extends HealthCheckService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  def getStatus: F[HealthCheckResult] = {
    for {
      hostname <- config.getHostname.recover { case _            => "" }
      dbStatus <- databaseService.checkStatus().recover { case _ => false }
    } yield {
      val success = hostname.nonEmpty && dbStatus
      HealthCheckResult(
        success,
        BuildInfo.version,
        hostname,
        dbStatus,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
  }
}
