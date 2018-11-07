package com.wanari.tutelar.util

import java.util.concurrent.atomic.AtomicLong

import cats.Applicative

class IdGeneratorCounterImpl[F[_]: Applicative] extends IdGenerator[F] {
  val counter = new AtomicLong(0)
  import cats.syntax.applicative._
  override def generate(): F[String] = counter.incrementAndGet().toString.pure
}
