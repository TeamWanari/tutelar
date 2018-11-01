package com.wanari.tutelar

import akka.actor.ActorSystem
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestBase extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterAll {

  val timeout = 1.second

  def await[T](f: Future[T]): T = Await.result(f, timeout)

  def useAS[R](block: ActorSystem => R): R = {
    val as = ActorSystem()
    try {
      block(as)
    } finally {
      await(as.terminate())
    }
  }
}
