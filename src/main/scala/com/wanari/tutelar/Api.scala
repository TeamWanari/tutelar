package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.wanari.tutelar.core.CoreApi
import com.wanari.tutelar.core.healthcheck.HealthCheckApi
import com.wanari.tutelar.providers.userpass.basic.BasicProviderApi
import com.wanari.tutelar.providers.userpass.email.EmailProviderApi
import com.wanari.tutelar.providers.userpass.ldap.LdapApi
import com.wanari.tutelar.providers.userpass.token.TotpApi
import com.wanari.tutelar.util.LoggerUtil.LogContext
import com.wanari.tutelar.util.TracingDirectives._
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import org.slf4j.Logger

import scala.concurrent.Future

trait Api {
  private val tracer: Tracer                                    = GlobalTracer.get()
  protected def withTrace(name: String): Directive1[LogContext] = trace(tracer, name).map(new LogContext(tracer, _))

  def route(): Route
}

object Api {
  val emptyRoute = Route(_.reject())

  def createRoute(api: Seq[Api]): Route = {
    api
      .map(_.route())
      .fold(Api.emptyRoute)(_ ~ _)
  }

  def createApi(services: Services[Future])(implicit logger: Logger): Route = {
    import com.wanari.tutelar.providers.oauth2.OAuth2Api._
    import services._
    import services.configService._

    val modules = configService.getEnabledModules
    logger.info(s"Load api for modules: ${modules.mkString(",")}")

    val api = modules.collect {
      case "health"   => new HealthCheckApi()
      case "github"   => new GithubApi()
      case "facebook" => new FacebookApi()
      case "google"   => new GoogleApi()
      case "ldap"     => new LdapApi()
      case "basic"    => new BasicProviderApi()
      case "email"    => new EmailProviderApi()
      case "totp"     => new TotpApi()
    } :+ new CoreApi

    cors() {
      createRoute(api)
    }
  }
}
