package com.wanari.tutelar.core
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{extractExecutionContext, onSuccess, redirect}
import akka.http.scaladsl.server.Route
import com.wanari.tutelar.Api
import com.wanari.tutelar.core.AuthService.Token
import com.wanari.tutelar.core.ProviderApi.{CallbackConfig, Errors}

import scala.concurrent.{ExecutionContext, Future}

trait ProviderApi extends Api {
  val callbackConfig: () => Future[CallbackConfig]

  def completeLoginFlow(token: Future[Token]): Route = {
    extractExecutionContext { implicit executor =>
      val callbackUrl = token
        .flatMap(generateCallbackUrl)
        .recoverWith { case _ => generateErrorCallbackUrl(Errors.AUTHENTICATION_FAILED) }

      onSuccess(callbackUrl) { url =>
        redirect(url, StatusCodes.Found)
      }
    }
  }

  private def generateCallbackUrl(token: Token)(implicit ec: ExecutionContext): Future[String] = {
    callbackConfig().map(_.success.replace("<<TOKEN>>", token))
  }

  private def generateErrorCallbackUrl(error: String)(implicit ec: ExecutionContext): Future[String] = {
    callbackConfig().map(_.failure.replace("<<ERROR>>", error))
  }
}

object ProviderApi {
  case class CallbackConfig(success: String, failure: String)

  object Errors {
    val AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
  }
}
