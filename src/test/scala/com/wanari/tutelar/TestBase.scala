package com.wanari.tutelar

import akka.actor.ActorSystem
import cats.MonadError
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.DatabaseService.{Account, User}
import com.wanari.tutelar.core.impl.AuthServiceImpl
import com.wanari.tutelar.core.impl.database.MemoryDatabaseService
import com.wanari.tutelar.core.impl.JwtServiceImpl.JwtConfig
import com.wanari.tutelar.core.{AuthService, ExpirationService, HookService, JwtService}
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.{DateTimeUtilCounterImpl, IdGeneratorCounterImpl}
import io.opentracing.noop.NoopTracerFactory
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import spray.json.{JsObject, JsString}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestBase extends AnyWordSpecLike with Matchers with MockitoSugar with BeforeAndAfterAll {
  implicit lazy val dummyLogContext = {
    val tracer = NoopTracerFactory.create()
    val span   = tracer.buildSpan("test").start()
    new LogContext(tracer, span)
  }

  val timeout = 1.second

  def await[T](f: Future[T]): T = Await.result(f, timeout)

  def withActorSystem[R](block: ActorSystem => R): R = {
    val as = ActorSystem()
    try {
      block(as)
    } finally {
      await(as.terminate())
    }
  }

  abstract class ProviderTestScope[F[_]: MonadError[*[_], Throwable]]() {
    val authType: String

    import cats.syntax.applicative._

    lazy val savedExternalId      = "ext_id"
    lazy val savedCustomData      = ""
    lazy val savedUser            = User("99999", 98765)
    lazy val savedAccount         = Account(authType, savedExternalId, savedUser.id, savedCustomData)
    lazy val providedData         = JsObject("userdata" -> JsString("helo"))
    lazy val hookResponseLogin    = JsObject("group" -> JsString("log"))
    lazy val hookResponseRegister = JsObject("group" -> JsString("reg"))

    implicit lazy val databaseService           = new MemoryDatabaseService[F]
    implicit lazy val idGenerator               = new IdGeneratorCounterImpl[F]
    implicit lazy val timeService               = new DateTimeUtilCounterImpl[F]
    implicit lazy val shortTermTokenServiceMock = mock[JwtService[F]]
    implicit lazy val longTermTokenServiceMock  = mock[JwtService[F]]
    implicit lazy val hookService               = mock[HookService[F]]

    def initDb(): F[Unit] = {
      databaseService.saveUser(savedUser)
      databaseService.saveAccount(savedAccount)
    }

    val authenticateResponse = TokenData("token", "refresh_token")
    when(shortTermTokenServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn("token".pure[F])
    when(longTermTokenServiceMock.encode(any[JsObject], any[Option[Duration]])).thenReturn("refresh_token".pure[F])

    when(hookService.register(any[String], any[String], any[String], any[JsObject])(any[LogContext]))
      .thenReturn(hookResponseRegister.pure[F])
    when(hookService.login(any[String], any[String], any[String], any[JsObject])(any[LogContext]))
      .thenReturn(hookResponseLogin.pure[F])

    implicit def dummyConfigFunction(name: String): JwtConfig = null

    implicit val expirationService = new ExpirationService[F] {
      override def isExpired(providerName: String, lastActivityAt: Long, loginAt: Long)(implicit
          ctx: LogContext
      ): F[Boolean] = false.pure[F]
    }

    implicit lazy val authService: AuthService[F] = new AuthServiceImpl[F]() {
      override protected val longTermTokenService: JwtService[F]  = longTermTokenServiceMock
      override protected val shortTermTokenService: JwtService[F] = shortTermTokenServiceMock
    }
  }
}
