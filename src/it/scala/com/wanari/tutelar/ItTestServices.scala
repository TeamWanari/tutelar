package com.wanari.tutelar

import com.wanari.tutelar.jwt.{JwtConfigService, JwtService, JwtServiceImpl}
import com.wanari.tutelar.ldap.LdapService
import com.wanari.tutelar.oauth2.{FacebookService, GithubService, GoogleService, OAuth2Service}

import scala.concurrent.{ExecutionContext, Future}

class ItTestServices(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  override implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  override implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  override implicit lazy val databaseService: DatabaseService[Future]       = new DatabaseServiceMemImpl[Future]

  implicit lazy val jwtConfig: JwtConfigService[Future]             = configService.getJwtConfig
  override implicit lazy val jwtService: Future[JwtService[Future]] = JwtServiceImpl.create

  override implicit lazy val idGenerator: IdGenerator[Future]         = new IdGeneratorCounterImpl[Future]
  override implicit lazy val dateTimeService: DateTimeService[Future] = new DateTimeServiceCounterImpl[Future]
  implicit lazy val authConfig: AuthConfigService[Future]             = configService.getAuthConfig
  override implicit lazy val authService: AuthService[Future]         = new AuthServiceImpl[Future]

  override implicit lazy val facebookService: FacebookService[Future] = null
  override implicit lazy val githubService: GithubService[Future]     = null
  override implicit lazy val googleService: GoogleService[Future]     = null

  def getOauthServiceByName(provider: String): OAuth2Service[Future] = {
    provider match {
      case "facebook" => facebookService
      case "github"   => githubService
      case "google"   => googleService
    }
  }

  override implicit lazy val ldapService: LdapService[Future] = null
}
