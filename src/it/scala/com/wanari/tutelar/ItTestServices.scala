package com.wanari.tutelar

import com.wanari.tutelar.github.GithubService
import com.wanari.tutelar.jwt.{JwtConfigService, JwtService, JwtServiceImpl}

import scala.concurrent.{ExecutionContext, Future}

class ItTestServices(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  override implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  override implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  override implicit lazy val databaseService: DatabaseService[Future]       = new DatabaseServiceMemImpl[Future]
  override implicit lazy val githubService: GithubService[Future]           = null

  implicit lazy val jwtConfig: JwtConfigService[Future]             = configService.getJwtConfig
  override implicit lazy val jwtService: Future[JwtService[Future]] = JwtServiceImpl.create

  override implicit lazy val idGenerator: IdGenerator[Future]         = new IdGeneratorCounterImpl[Future]
  override implicit lazy val dateTimeService: DateTimeService[Future] = new DateTimeServiceCounterImpl[Future]
  implicit lazy val authConfig: AuthConfigService[Future]             = configService.getAuthConfig
  override implicit lazy val authService: AuthService[Future]         = new AuthServiceImpl[Future]
}
