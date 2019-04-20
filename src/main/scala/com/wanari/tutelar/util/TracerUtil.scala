package com.wanari.tutelar.util

import io.jaegertracing.Configuration
import io.jaegertracing.Configuration._
import io.opentracing.mock.MockTracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.util.GlobalTracer

// todo convert to a service
object TracerUtil {
  def initJaeger(): Unit = {
    // todo from config
    val samplerConfig  = SamplerConfiguration.fromEnv().withType("const").withParam(1)
    val reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true)
    val config         = new Configuration("tutelar").withSampler(samplerConfig).withReporter(reporterConfig)

    GlobalTracer.registerIfAbsent(config.getTracer)
  }

  def initMock(): Unit = {
    GlobalTracer.registerIfAbsent(new MockTracer())
  }

  def initNoop(): Unit = {
    GlobalTracer.registerIfAbsent(NoopTracerFactory.create())
  }
}
