package com.wanari.tutelar

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.wanari.tutelar.github.GithubService
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

trait RouteTestBase extends WordSpecLike with Matchers with ScalatestRouteTest with MockitoSugar {
  trait BaseTestScope {
    lazy val services = new ItTestServices {
      override implicit lazy val githubService: GithubService[Future] = mock[GithubService[Future]]
    }
    lazy val route: Route = Api.createApi(services)
  }
}
