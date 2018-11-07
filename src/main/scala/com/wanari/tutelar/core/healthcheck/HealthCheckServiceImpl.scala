package com.wanari.tutelar.core.healthcheck

import cats.MonadError
import com.wanari.tutelar.core.{ConfigService, DatabaseService}
import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]](
    implicit F: MonadError[F, Throwable],
    config: ConfigService[F],
    databaseService: DatabaseService[F]
) extends HealthCheckService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  def getStatus: F[HealthCheckResult] = {
    for {
      version  <- config.getVersion.recover { case _             => "" }
      hostname <- config.getHostname.recover { case _            => "" }
      dbStatus <- databaseService.checkStatus().recover { case _ => false }
    } yield {
      val success = !(version.isEmpty || hostname.isEmpty) && dbStatus
      HealthCheckResult(success, version, hostname, dbStatus)
    }
  }
}
