package com.wanari.tutelar.util

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{extractRequest, mapResponse, provide}
import io.opentracing.{Span, Tracer}
import io.opentracing.propagation.Format

import scala.util.Try

// source: https://gist.github.com/chadselph/65f21fc86f873d6569f4cfe4f96ce036
trait TracingDirectives {

  def trace(tracer: Tracer, operationName: String): Directive1[Span] = extractRequest.flatMap { req =>
    val parent = Try(
      // This method will throw an IllegalArgumentException for a bad
      // tracer header, or return null for no header. Handle both cases as None
      tracer.extract(Format.Builtin.HTTP_HEADERS, new AkkaHttpHeaderExtractor(req.headers))
    ).filter(_ != null).toOption
    val span = parent.fold(tracer.buildSpan(operationName).start())(
      p => tracer.buildSpan(operationName).asChildOf(p).start()
    )
    mapResponse { resp =>
      span.setTag("http.status_code", resp.status.intValue())
      span.setTag("http.url", req.effectiveUri(securedConnection = false).toString())
      span.setTag("http.method", req.method.value)
      span.finish()

      resp
    } & provide(span)
  }
}

object TracingDirectives extends TracingDirectives
