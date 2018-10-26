package com.wanari.tutelar

import cats.MonadError
import com.wanari.tutelar.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]](
    implicit F: MonadError[F, Throwable],
    config: ConfigService[F],
    dataBaseService: DataBaseService[F]
) extends HealthCheckService[F] {
  import cats.syntax.functor._
  import cats.syntax.flatMap._
  import cats.syntax.applicativeError._

  def getStatus: F[HealthCheckResult] = {
    for {
      version  <- config.getVersion.recover { case _             => "" }
      hostname <- config.getHostname.recover { case _            => "" }
      dbStatus <- dataBaseService.checkStatus().recover { case _ => false }
    } yield {
      val success = !(version.isEmpty || hostname.isEmpty) && dbStatus
      HealthCheckResult(success, version, hostname, dbStatus)
    }
  }
}

trait HealthCheckService[F[_]] {
  def getStatus: F[HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(success: Boolean, version: String, hostname: String, database: Boolean)
  implicit val healthCheckResultFormat = jsonFormat4(HealthCheckResult)
}
