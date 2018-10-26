package com.wanari.tutelar

import scala.concurrent.{ExecutionContext, Future}

class ItTestServices(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  override implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  override implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  override implicit lazy val dataBaseService: DataBaseService[Future]       = new DataBaseServiceMemImpl[Future]
}
