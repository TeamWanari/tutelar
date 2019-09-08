package com.wanari.tutelar

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait AwaitUtil {
  implicit val timeout = 5.seconds

  def await[T](f: Future[T]): T = Await.result(f, timeout)
}
