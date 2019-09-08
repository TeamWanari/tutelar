package com.wanari.tutelar.core.healthcheck

import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult
import spray.json.RootJsonFormat

trait HealthCheckService[F[_]] {
  def getStatus: ErrorOr[F, HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(
      success: Boolean,
      version: String,
      database: Boolean,
      buildAtString: String,
      buildAtMillis: Long,
      commitHash: Option[String]
  )
  implicit val healthCheckResultFormat: RootJsonFormat[HealthCheckResult] = jsonFormat6(HealthCheckResult)
}
