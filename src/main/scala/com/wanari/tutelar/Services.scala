package com.wanari.tutelar

import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.MonadError
import com.wanari.tutelar.core._
import com.wanari.tutelar.core.config.{ServerConfig, ServerConfigImpl}
import com.wanari.tutelar.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import com.wanari.tutelar.core.impl.database._
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl
import com.wanari.tutelar.core.impl.{AuthServiceImpl, CsrfServiceNotChecked, HookServiceImpl, RabbitMqServiceImpl}
import com.wanari.tutelar.providers.oauth2.{FacebookService, GithubService, GoogleService}
import com.wanari.tutelar.providers.userpass.{PasswordDifficultyChecker, PasswordDifficultyCheckerImpl}
import com.wanari.tutelar.providers.userpass.basic.{BasicProviderService, BasicProviderServiceImpl}
import com.wanari.tutelar.providers.userpass.email._
import com.wanari.tutelar.providers.userpass.ldap.{LdapService, LdapServiceImpl}
import com.wanari.tutelar.providers.userpass.token.{TotpService, TotpServiceImpl}
import com.wanari.tutelar.util._
import org.slf4j.Logger
import reactivemongo.api.MongoDriver

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait Services[F[_]] {
  implicit val configService: ServerConfig[F]
  implicit val healthCheckService: HealthCheckService[F]
  implicit val facebookService: FacebookService[F]
  implicit val githubService: GithubService[F]
  implicit val googleService: GoogleService[F]
  implicit val databaseService: DatabaseService[F]
  implicit val jwtService: JwtService[F]
  implicit val idGenerator: IdGenerator[F]
  implicit val dateTimeService: DateTimeUtil[F]
  implicit val hookService: HookService[F]
  implicit val authService: AuthService[F]
  implicit val ldapService: LdapService[F]
  implicit val basicLoginService: BasicProviderService[F]
  implicit val emailService: EmailService[F]
  implicit val emailLoginService: EmailProviderService[F]
  implicit val totpService: TotpService[F]
  implicit val passwordDifficultyChecker: PasswordDifficultyChecker[F]
  implicit val tracerService: TracerService[F]
  implicit val rabbitMqService: RabbitMqService[F]

  def init()(implicit logger: Logger, ev: MonadError[F, Throwable]): F[Unit] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    import Initable._

    logger.info("Init services")
    for {
      _ <- initialize(configService, "config")
      _ <- initialize(tracerService, "tracer")
      _ <- initialize(databaseService, "database")
      _ <- initialize(jwtService, "jwt")
      _ <- initializeIfEnabled(rabbitMqService, "rabbitmq")
      _ <- initializeIfEnabled(ldapService, "ldap")
    } yield ()
  }
}

class RealServices(implicit ec: ExecutionContext, actorSystem: ActorSystem, materializer: Materializer)
    extends Services[Future] {

  import cats.instances.future._

  implicit lazy val configService: ServerConfig[Future] = new ServerConfigImpl[Future]
  import configService.runtimeConfig._
  import configService._

  implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  implicit lazy val mongoDriver: MongoDriver                       = new MongoDriver()
  implicit lazy val databaseService: DatabaseService[Future]       = DatabaseServiceFactory.create()
  implicit lazy val httpWrapper: HttpWrapper[Future]               = new AkkaHttpWrapper()
  implicit lazy val csrfService: CsrfService[Future]               = new CsrfServiceNotChecked[Future]
  implicit lazy val facebookService: FacebookService[Future] =
    new FacebookService[Future](configService.runtimeConfig.facebookConfig)
  implicit lazy val githubService: GithubService[Future] =
    new GithubService[Future](configService.runtimeConfig.githubConfig)
  implicit lazy val googleService: GoogleService[Future] =
    new GoogleService[Future](configService.runtimeConfig.googleConfig)
  implicit lazy val jwtService: JwtService[Future]                  = new JwtServiceImpl[Future]
  implicit lazy val idGenerator: IdGenerator[Future]                = new IdGeneratorImpl[Future]
  implicit lazy val dateTimeService: DateTimeUtil[Future]           = new DateTimeUtilImpl[Future]
  implicit lazy val hookService: HookService[Future]                = new HookServiceImpl[Future]
  implicit lazy val authService: AuthService[Future]                = new AuthServiceImpl[Future]
  implicit lazy val ldapService: LdapService[Future]                = new LdapServiceImpl
  implicit lazy val basicLoginService: BasicProviderService[Future] = new BasicProviderServiceImpl[Future]()
  // TODO remove await
  implicit lazy val emailService: EmailService[Future]              = Await.result(EmailServiceFactory.create[Future](), 1.second)
  implicit lazy val emailLoginService: EmailProviderService[Future] = new EmailProviderServiceImpl[Future]()
  implicit lazy val totpService: TotpService[Future]                = new TotpServiceImpl[Future]()
  implicit lazy val passwordDifficultyChecker: PasswordDifficultyChecker[Future] =
    new PasswordDifficultyCheckerImpl[Future]
  implicit lazy val tracerService: TracerService[Future]         = new TracerService[Future]()
  override implicit val rabbitMqService: RabbitMqService[Future] = new RabbitMqServiceImpl[Future]()
}
