package com.wanari.tutelar
import cats.Applicative

trait DateTimeService[F[_]] {
  def getCurrentTimeMillis(): F[Long]
}

class DateTimeServiceImpl[F[_]: Applicative] extends DateTimeService[F] {
  import cats.syntax.applicative._
  override def getCurrentTimeMillis(): F[Long] = System.currentTimeMillis().pure
}
