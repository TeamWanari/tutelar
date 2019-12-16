package com.wanari.tutelar

import akka.actor.ActorSystem
import cats.MonadError
import com.wanari.tutelar.core._
import com.wanari.tutelar.core.impl._
import com.wanari.tutelar.core.impl.database._
import com.wanari.tutelar.providers.oauth2.{FacebookService, GithubService, GoogleService}
import com.wanari.tutelar.providers.userpass.basic.{BasicProviderService, BasicProviderServiceImpl}
import com.wanari.tutelar.providers.userpass.email._
import com.wanari.tutelar.providers.userpass.ldap.{LdapService, LdapServiceImpl}
import com.wanari.tutelar.providers.userpass.token.{TotpService, TotpServiceImpl}
import com.wanari.tutelar.providers.userpass.{PasswordDifficultyChecker, PasswordDifficultyCheckerImpl}
import com.wanari.tutelar.util._
import org.slf4j.Logger
import reactivemongo.api.AsyncDriver

import scala.concurrent.{ExecutionContext, Future}

trait Services[F[_]] {
  implicit val configService: ConfigService
  implicit def healthCheckService: HealthCheckService[F]
  implicit def facebookService: FacebookService[F]
  implicit def githubService: GithubService[F]
  implicit def googleService: GoogleService[F]
  implicit def databaseService: DatabaseService[F]
  implicit def idGenerator: IdGenerator[F]
  implicit def dateTimeService: DateTimeUtil[F]
  implicit def hookService: HookService[F]
  implicit def authService: AuthService[F]
  implicit def ldapService: LdapService[F]
  implicit def basicLoginService: BasicProviderService[F]
  implicit def emailService: EmailService[F]
  implicit def emailLoginService: EmailProviderService[F]
  implicit def totpService: TotpService[F]
  implicit def passwordDifficultyChecker: PasswordDifficultyChecker[F]
  implicit def tracerService: TracerService[F]
  implicit def amqpService: AmqpService[F]
  implicit def escherService: EscherService[F]
  implicit def expirationService: ExpirationService[F]

  def init()(implicit logger: Logger, ev: MonadError[F, Throwable]): F[Unit] = {
    import Initable._
    import cats.syntax.flatMap._
    import cats.syntax.functor._

    logger.info("Init services...")
    for {
      _ <- initialize(tracerService, "tracer")
      _ <- initialize(databaseService, "database")
      _ <- initialize(authService, "auth_service")
      _ <- initialize(escherService, "escher")
      _ <- initializeIfEnabled(amqpService, "ampq")
      _ <- initializeIfEnabled(emailLoginService, "email")
      _ <- initializeIfEnabled(totpService, "totp")
      _ <- initializeIfEnabled(ldapService, "ldap")
    } yield {
      logger.info("Init services DONE")
    }
  }
}

class RealServices(implicit ec: ExecutionContext, actorSystem: ActorSystem) extends Services[Future] {
  import cats.instances.future._

  implicit lazy val configService: ConfigService = new ConfigServiceImpl
  import configService._

  implicit lazy val healthCheckService: HealthCheckService[Future]  = new HealthCheckServiceImpl[Future]
  implicit lazy val mongoDriver: AsyncDriver                        = new AsyncDriver()
  implicit lazy val databaseService: DatabaseService[Future]        = DatabaseServiceFactory.create()
  implicit lazy val httpWrapper: HttpWrapper[Future]                = new AkkaHttpWrapper()
  implicit lazy val csrfService: CsrfService[Future]                = new CsrfServiceNotChecked[Future]
  implicit lazy val facebookService: FacebookService[Future]        = new FacebookService[Future](configService.facebookConfig)
  implicit lazy val githubService: GithubService[Future]            = new GithubService[Future](configService.githubConfig)
  implicit lazy val googleService: GoogleService[Future]            = new GoogleService[Future](configService.googleConfig)
  implicit lazy val idGenerator: IdGenerator[Future]                = new IdGeneratorImpl[Future]
  implicit lazy val dateTimeService: DateTimeUtil[Future]           = new DateTimeUtilImpl[Future]
  implicit lazy val hookService: HookService[Future]                = new HookServiceImpl[Future]
  implicit lazy val authService: AuthService[Future]                = new AuthServiceImpl[Future]
  implicit lazy val ldapService: LdapService[Future]                = new LdapServiceImpl
  implicit lazy val basicLoginService: BasicProviderService[Future] = new BasicProviderServiceImpl[Future]()
  implicit lazy val emailService: EmailService[Future]              = EmailServiceFactory.create[Future]()
  implicit lazy val emailLoginService: EmailProviderService[Future] = new EmailProviderServiceImpl[Future]()
  implicit lazy val totpService: TotpService[Future]                = new TotpServiceImpl[Future]()
  implicit lazy val tracerService: TracerService[Future]            = new TracerService[Future]()
  implicit lazy val amqpService: AmqpService[Future]                = new AmqpServiceImpl[Future]()
  implicit lazy val escherService: EscherService[Future]            = new EscherServiceImpl()
  implicit lazy val expirationService: ExpirationService[Future]    = new ExpirationServiceImpl[Future]()
  implicit lazy val passwordDifficultyChecker: PasswordDifficultyChecker[Future] =
    new PasswordDifficultyCheckerImpl[Future]
}
