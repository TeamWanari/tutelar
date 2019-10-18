package com.wanari.tutelar.core

import scala.concurrent.duration.Duration

object ExpirationService {
  sealed trait ExpirationConfig
  case object ExpirationDisabled                      extends ExpirationConfig
  case class ExpirationInactivity(duration: Duration) extends ExpirationConfig
  case class ExpirationLifetime(duration: Duration)   extends ExpirationConfig
}
