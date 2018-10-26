package com.wanari.tutelar

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

trait RouteTestBase extends WordSpecLike with Matchers with ScalatestRouteTest with MockitoSugar {
  trait BaseTestScope {
    lazy val services     = new ItTestServices()
    lazy val route: Route = Api.createApi(services)
  }
}
