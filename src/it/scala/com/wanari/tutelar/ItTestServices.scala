package com.wanari.tutelar

import com.wanari.tutelar.github.GithubService

import scala.concurrent.{ExecutionContext, Future}

class ItTestServices(githubServiceMock: GithubService[Future])(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  override implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  override implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  override implicit lazy val databaseService: DatabaseService[Future]       = new DatabaseServiceMemImpl[Future]
  override implicit lazy val githubService: GithubService[Future]           = githubServiceMock
}
