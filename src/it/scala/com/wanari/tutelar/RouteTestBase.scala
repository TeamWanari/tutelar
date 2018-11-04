package com.wanari.tutelar

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.wanari.tutelar.ldap.LdapService
import com.wanari.tutelar.oauth2.{FacebookService, GithubService, GoogleService}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

trait RouteTestBase extends WordSpecLike with Matchers with ScalatestRouteTest with MockitoSugar {
  trait BaseTestScope {
    lazy val services = new ItTestServices {
      override implicit lazy val facebookService: FacebookService[Future] = mock[FacebookService[Future]]
      override implicit lazy val githubService: GithubService[Future]     = mock[GithubService[Future]]
      override implicit lazy val googleService: GoogleService[Future]     = mock[GoogleService[Future]]
      override implicit lazy val ldapService: LdapService[Future]         = mock[LdapService[Future]]

      when(facebookService.TYPE) thenReturn "facebook"
      when(githubService.TYPE) thenReturn "github"
      when(googleService.TYPE) thenReturn "google"
    }
    lazy val route: Route = Api.createApi(services)
  }
}
