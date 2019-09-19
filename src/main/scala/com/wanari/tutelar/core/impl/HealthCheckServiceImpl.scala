package com.wanari.tutelar.core.impl

import cats.Monad
import cats.data.EitherT
import com.wanari.tutelar.BuildInfo
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.core.HealthCheckService.HealthCheckResult
import com.wanari.tutelar.core.{DatabaseService, HealthCheckService}

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
