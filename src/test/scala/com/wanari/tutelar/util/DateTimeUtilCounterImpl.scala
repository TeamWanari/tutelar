package com.wanari.tutelar.util

import java.util.concurrent.atomic.AtomicLong

import cats.Applicative

class DateTimeUtilCounterImpl[F[_]: Applicative] extends DateTimeUtil[F] {
  val counter = new AtomicLong(0)
  import cats.syntax.applicative._
  override def getCurrentTimeMillis(): F[Long] = counter.incrementAndGet().pure
}
