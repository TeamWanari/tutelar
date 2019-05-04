package com.wanari.tutelar

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.wanari.tutelar.util.LoggerUtil
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {
  LoggerUtil.initBridge()

  implicit lazy val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

  implicit lazy val system           = ActorSystem("tutelar-system")
  implicit lazy val materializer     = ActorMaterializer()
  implicit lazy val executionContext = system.dispatcher
  import cats.instances.future._

  val services = new RealServices()

  val starting = for {
    _ <- services.init()
    route = Api.createApi(services)
    server <- Http().bindAndHandle(route, "0.0.0.0", 9000)
  } yield {
    setupShutdownHook(server)
  }

  starting.onComplete {
    case Success(_) => logger.info("LoginService started")
    case Failure(ex) =>
      logger.error("LoginService starting failed", ex)
      system.terminate()
  }

  def setupShutdownHook(server: Http.ServerBinding): Unit = {
    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
      logger.info("LoginService shutting down...")
      server.terminate(hardDeadline = 8.seconds).map(_ => Done)
    }
  }
}
