package com.wanari.tutelar

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.util.{Failure, Success}

object Main extends App {
  LoggerUtil.initBridge()

  lazy val logger = LoggerUtil.getDefaultLogger

  implicit lazy val system           = ActorSystem("tutelar-system")
  implicit lazy val materializer     = ActorMaterializer()
  implicit lazy val executionContext = system.dispatcher

  val services = new RealServices()

  val route = Api.createApi(services)

  Http().bindAndHandle(route, "0.0.0.0", 9000).onComplete {
    case Success(_)  => logger.info("LoginService started")
    case Failure(ex) => logger.error("LoginService starting failed", ex)
  }
}
