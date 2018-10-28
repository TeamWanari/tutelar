package com.wanari.tutelar.github

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.{CsrfService, TestBase}
import spray.json.RootJsonReader

import scala.reflect.ClassTag
import scala.util.Try

class GithubServiceSpec extends TestBase {
  import cats.instances.try_._

  trait Scope {
    implicit lazy val config = new GithubConfigService[Try] {
      override def getClientId: Try[String] = Try("clientId")

      override def getClientSecret: Try[String] = Try("secret")

      override def getScopes: Try[Seq[String]] = Try(Seq("a", "b", "c"))
    }
    implicit lazy val csrfService = new CsrfService[Try] {
      override def getCsrfToken(auther: String): Try[String] = Try("csrftoken")

      override def checkCsrfToken(auther: String, str: String): Try[Unit] = Try({})
    }
    implicit lazy val httpWrapper = new HttpWrapper[Try] {
      override def singleRequest(httpRequest: HttpRequest): Try[HttpResponse] = Try(reqResp(httpRequest))

      override def unmarshalEntityTo[T: ClassTag: RootJsonReader](resp: HttpResponse): Try[T] = ???
    }
    lazy val service = new GithubServiceImpl[Try]

    def reqResp(httpRequest: HttpRequest): HttpResponse = ???
  }

  "#generateIdentifierUrl" in new Scope {
    service.generateIdentifierUrl.get.toString shouldBe "https://github.com/login/oauth/authorize?client_id=clientId&scope=a+b+c&state=csrftoken"
  }

}
