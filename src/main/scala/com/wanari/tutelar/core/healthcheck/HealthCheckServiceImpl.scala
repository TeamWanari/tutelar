package com.wanari.tutelar.core.healthcheck

import cats.data.EitherT
import cats.Monad
import com.wanari.tutelar.BuildInfo
import com.wanari.tutelar.core.DatabaseService
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]: Monad](implicit databaseService: DatabaseService[F]) extends HealthCheckService[F] {

  def getStatus: ErrorOr[F, HealthCheckResult] = {
    for {
      dbStatus <- EitherT.right(databaseService.checkStatus())
    } yield {
      val success = dbStatus
      HealthCheckResult(
        success,
        BuildInfo.version,
        dbStatus,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
  }
}
