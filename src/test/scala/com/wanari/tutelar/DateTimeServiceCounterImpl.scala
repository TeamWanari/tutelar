package com.wanari.tutelar

import java.util.concurrent.atomic.AtomicLong

import cats.Applicative

class DateTimeServiceCounterImpl[F[_]: Applicative] extends DateTimeService[F] {
  val counter = new AtomicLong(0)
  import cats.syntax.applicative._
  override def getCurrentTimeMillis(): F[Long] = counter.incrementAndGet().pure
}
