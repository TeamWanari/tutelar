package com.wanari.tutelar

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import com.wanari.tutelar.util.LoggerUtil
import io.opentracing.util.GlobalTracer
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {
  LoggerUtil.initBridge()

  private implicit lazy val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

  private implicit lazy val system           = ActorSystem("tutelar-system")
  private implicit lazy val executionContext = system.dispatcher
  import cats.instances.future._

  private val starting = for {
    services <- Future(new RealServices())
    _        <- services.init()
    route = Api.createApi(services)
    server <- Http().bindAndHandle(route, "0.0.0.0", 9000)
  } yield {
    setupShutdownHook(server)
  }

  starting.onComplete {
    case Success(_) => logger.info("Tutelar started")
    case Failure(ex) =>
      logger.error("Tutelar starting failed", ex)
      GlobalTracer.get().close()
      system.terminate()
  }

  private def setupShutdownHook(server: Http.ServerBinding): Unit = {
    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
      logger.info("Tutelar shutting down...")
      server.terminate(hardDeadline = 8.seconds).map(_ => Done)
    }
  }
}
