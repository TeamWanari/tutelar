package com.wanari.tutelar

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.wanari.tutelar.jwt.{JwtConfigService, JwtService, JwtServiceImpl}
import com.wanari.tutelar.oauth2.{FacebookService, GithubService, GoogleService}
import com.wanari.tutelar.util.{AkkaHttpWrapper, HttpWrapper}

import scala.concurrent.{ExecutionContext, Future}

trait Services[F[_]] {
  implicit val configService: ConfigService[F]
  implicit val healthCheckService: HealthCheckService[F]
  implicit val facebookService: FacebookService[F]
  implicit val githubService: GithubService[F]
  implicit val googleService: GoogleService[F]
  implicit val databaseService: DatabaseService[F]
  implicit val jwtService: F[JwtService[F]]
  implicit val idGenerator: IdGenerator[F]
  implicit val dateTimeService: DateTimeService[F]
  implicit val authService: AuthService[F]
}

class RealServices(implicit ec: ExecutionContext, actorSystem: ActorSystem, materializer: Materializer)
    extends Services[Future] {

  import cats.instances.future._

  implicit lazy val configService: ConfigService[Future]           = new ConfigServiceImpl[Future]
  implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  implicit lazy val databaseService: DatabaseService[Future]       = new DatabaseServiceImpl(DatabaseServiceImpl.getDatabase)
  implicit lazy val httpWrapper: HttpWrapper[Future]               = new AkkaHttpWrapper()
  implicit lazy val csrfService: CsrfService[Future]               = new CsrfServiceNotChecked[Future]
  implicit lazy val facebookService: FacebookService[Future] =
    new FacebookService[Future](configService.getFacebookConfig)
  implicit lazy val githubService: GithubService[Future]     = new GithubService[Future](configService.getGithubConfig)
  implicit lazy val googleService: GoogleService[Future]     = new GoogleService[Future](configService.getGoogleConfig)
  implicit lazy val jwtConfig: JwtConfigService[Future]      = configService.getJwtConfig
  implicit lazy val jwtService: Future[JwtService[Future]]   = JwtServiceImpl.create
  implicit lazy val idGenerator: IdGenerator[Future]         = new IdGeneratorImpl[Future]
  implicit lazy val dateTimeService: DateTimeService[Future] = new DateTimeServiceImpl[Future]
  implicit lazy val authConfig: AuthConfigService[Future]    = configService.getAuthConfig
  implicit lazy val authService: AuthService[Future]         = new AuthServiceImpl[Future]
}
