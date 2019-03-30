package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.healthcheck.HealthCheckApi
import com.wanari.tutelar.providers.userpass.basic.BasicProviderApi
import com.wanari.tutelar.providers.userpass.email.EmailProviderApi
import com.wanari.tutelar.providers.userpass.ldap.LdapApi
import com.wanari.tutelar.providers.userpass.token.TotpApi
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

trait Api {
  def route(): Route
}

object Api {
  val emptyRoute = Route(_.reject())

  def createRoute(api: Seq[Api]): Route = {
    api
      .map(_.route())
      .fold(Api.emptyRoute)(_ ~ _)
  }

  def createApi(services: Services[Future])(implicit ec: ExecutionContext, logger: Logger): Future[Route] = {
    import com.wanari.tutelar.providers.oauth2.OAuth2Api._
    import services._
    import services.configService.runtimeConfig._

    configService.getEnabledModules
      .map { modules =>
        logger.info(s"Load api for modules: ${modules.mkString(",")}")
        modules.collect {
          case "github"   => new GithubApi()
          case "facebook" => new FacebookApi()
          case "google"   => new GoogleApi()
          case "ldap"     => new LdapApi()
          case "basic"    => new BasicProviderApi()
          case "email"    => new EmailProviderApi()
          case "totp"     => new TotpApi()
        }
      }
      .map(_ :+ new HealthCheckApi())
      .map { api =>
        cors() {
          createRoute(api)
        }
      }
  }
}
