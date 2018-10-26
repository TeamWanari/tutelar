package com.wanari.tutelar

import scala.concurrent.{ExecutionContext, Future}

trait Services[F[_]] {
  implicit val configService: ConfigService[F]
  implicit val healthCheckService: HealthCheckService[F]
  implicit val databaseService: DatabaseService[F]
}

class RealServices(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  implicit lazy val databaseService: DatabaseService[Future]       = new DatabaseServiceImpl(DatabaseServiceImpl.getDatabase)
}
