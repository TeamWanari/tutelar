package com.wanari.tutelar.core.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import com.emarsys.escher.akka.http.EscherDirectives
import com.emarsys.escher.akka.http.config.EscherConfig
import com.wanari.tutelar.core.EscherService

import scala.concurrent.{ExecutionContext, Future}

class EscherServiceImpl(implicit config: EscherConfig, actorSystem: ActorSystem) extends EscherService[Future] {
  import actorSystem.dispatcher

  private lazy val escher = new EscherDirectives {
    override val escherConfig: EscherConfig = config
  }

  override def init: Future[Unit] = {
    Future(escher)
  }

  override def signRequest(serviceName: String, request: HttpRequest): Future[HttpRequest] = {
    escher.signRequest(serviceName)(implicitly[ExecutionContext], implicitly[Materializer])(request)
  }
}
