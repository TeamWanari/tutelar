package com.wanari.tutelar.util

import java.io.PrintStream
import java.util.UUID
import java.util.logging.Level

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import io.opentracing.propagation.{Format, TextMapAdapter}
import io.opentracing.{Span, Tracer}
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import scala.collection.immutable

object LoggerUtil {
  def initBridge(): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    java.util.logging.Logger.getLogger("").setLevel(Level.FINEST)
    System.setErr(new PrintStream((b: Int) => {}))
  }

  class LogContext(val tracer: Tracer, val span: Span) {
    val requestId: String = UUID.randomUUID().toString
    span.setTag("requestId", requestId)

    def getInjectHeaders: immutable.Seq[HttpHeader] = {
      import scala.jdk.CollectionConverters._
      val collector = new java.util.HashMap[String, String]()
      tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapAdapter(collector))
      immutable.Seq(collector.asScala.map(t => RawHeader(t._1, t._2)).toSeq: _*)
    }
  }

  class Logger(name: String) {
    private lazy val logger = LoggerFactory.getLogger(name)

    def debug(msg: String)(implicit ctx: LogContext): Unit = _debug(msg, None)

    def debug(msg: String, ex: Throwable)(implicit ctx: LogContext): Unit = _debug(msg, Some(ex))

    def info(msg: String)(implicit ctx: LogContext): Unit = _info(msg, None)

    def info(msg: String, ex: Throwable)(implicit ctx: LogContext): Unit = _info(msg, Some(ex))

    def warn(msg: String)(implicit ctx: LogContext): Unit = _warn(msg, None)

    def warn(msg: String, ex: Throwable)(implicit ctx: LogContext): Unit = _warn(msg, Some(ex))

    def error(msg: String)(implicit ctx: LogContext): Unit = _error(msg, None)

    def error(msg: String, ex: Throwable)(implicit ctx: LogContext): Unit = _error(msg, Some(ex))

    private def _debug(msg: String, ex: Option[Throwable])(implicit ctx: LogContext): Unit = {
      if (logger.isDebugEnabled) {
        ctx.span.log(msg)
        val marker = createRequestIdMarker()
        ex.fold(logger.debug(marker, msg))(logger.debug(marker, msg, _))
      }
    }

    private def _info(msg: String, ex: Option[Throwable])(implicit ctx: LogContext): Unit = {
      if (logger.isInfoEnabled) {
        ctx.span.log(msg)
        val marker = createRequestIdMarker()
        ex.fold(logger.info(marker, msg))(logger.info(marker, msg, _))
      }
    }

    private def _warn(msg: String, ex: Option[Throwable])(implicit ctx: LogContext): Unit = {
      if (logger.isWarnEnabled) {
        ctx.span.log(msg)
        val marker = createRequestIdMarker()
        ex.fold(logger.warn(marker, msg))(logger.warn(marker, msg, _))
      }
    }

    private def _error(msg: String, ex: Option[Throwable])(implicit ctx: LogContext): Unit = {
      if (logger.isErrorEnabled) {
        ctx.span.log(msg)
        val marker = createRequestIdMarker()
        ex.fold(logger.error(marker, msg))(logger.error(marker, msg, _))
      }
    }

    private def createRequestIdMarker()(implicit ctx: LogContext) = {
      Markers.append("requestId", ctx.requestId)
    }
  }
}
