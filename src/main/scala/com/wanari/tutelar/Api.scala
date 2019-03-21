package com.wanari.tutelar

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.core.healthcheck.HealthCheckApi
import com.wanari.tutelar.providers.userpass.basic.BasicProviderApi
import com.wanari.tutelar.providers.userpass.email.EmailProviderApi
import com.wanari.tutelar.providers.userpass.ldap.LdapApi

import scala.concurrent.Future

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

  def createApi(services: Services[Future]): Route = {
    import com.wanari.tutelar.providers.oauth2.OAuth2Api._
    import services._
    import services.configService.runtimeConfig._

    val api = Seq(
      new HealthCheckApi(),
      new GithubApi(),
      new FacebookApi(),
      new GoogleApi(),
      new LdapApi(),
      new BasicProviderApi(),
      new EmailProviderApi()
    )

    createRoute(api)
  }
}
