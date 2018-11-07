package com.wanari.tutelar.core.healthcheck

import com.wanari.tutelar.core.healthcheck.HealthCheckService.HealthCheckResult

trait HealthCheckService[F[_]] {
  def getStatus: F[HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(success: Boolean, version: String, hostname: String, database: Boolean)
  implicit val healthCheckResultFormat = jsonFormat4(HealthCheckResult)
}
