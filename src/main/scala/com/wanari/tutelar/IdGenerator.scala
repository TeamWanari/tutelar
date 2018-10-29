package com.wanari.tutelar
import java.util.UUID

import cats.Applicative

trait IdGenerator[F[_]] {
  def generate(): F[String]
}

class IdGeneratorImpl[F[_]: Applicative] extends IdGenerator[F] {
  import cats.syntax.applicative._
  override def generate(): F[String] = UUID.randomUUID().toString.pure
}
