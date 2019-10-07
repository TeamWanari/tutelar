package com.wanari.tutelar.core

import akka.http.scaladsl.model.HttpRequest
import com.wanari.tutelar.Initable

trait EscherService[F[_]] extends Initable[F] {
  def signRequest(service: String, request: HttpRequest): F[HttpRequest]
}
