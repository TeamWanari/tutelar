package com.wanari.tutelar.core

import cats.MonadError
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.TracerService.TracerServiceConfig
import com.wanari.tutelar.core.Errors.WrongConfig
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.util.GlobalTracer

class TracerService[F[_]: MonadError[*[_], Throwable]](
    implicit config: TracerServiceConfig,
    configService: ConfigService
) extends Initable[F] {
  import cats.syntax.applicative._
  import com.wanari.tutelar.util.ApplicativeErrorSyntax._

  override def init: F[Unit] = {
    config.client.toLowerCase match {
      case TracerService.OFF    => initNoop().pure
      case TracerService.JAEGER => initJaeger().pure
      case _                    => WrongConfig(s"Unsupported Tracer client: ${config.client}").raise
    }
  }

  private def initNoop(): Unit = {
    GlobalTracer.registerIfAbsent(NoopTracerFactory.create())
  }

  private def initJaeger(): Unit = {
    GlobalTracer.registerIfAbsent(configService.jaegerConfig.getTracer)
  }

}

object TracerService {
  val OFF: String    = "off"
  val JAEGER: String = "jaeger"
  case class TracerServiceConfig(client: String)
}
