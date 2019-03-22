package com.wanari.tutelar.providers.userpass.email

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import cats.MonadError
import com.wanari.tutelar.providers.userpass.email.EmailProviderService.EmailProviderConfig
import com.wanari.tutelar.providers.userpass.email.EmailServiceImpl.EmailRequestData
import com.wanari.tutelar.util.HttpWrapper
import spray.json._

class EmailServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit http: HttpWrapper[F],
    configF: () => F[EmailProviderConfig]
) extends EmailService[F] {

  override def sendRegisterUrl(email: String, url: String): F[Unit] = {
    send(EmailRequestData(email, "register", JsObject(), JsObject("registrationUrl" -> JsString(url))))
  }

  override def sendResetPasswordUrl(email: String, url: String): F[Unit] = {
    send(EmailRequestData(email, "reset-password", JsObject(), JsObject("resetPasswordUrl" -> JsString(url))))
  }

  private def send(data: EmailRequestData): F[Unit] = {
    import cats.syntax.flatMap._
    import cats.syntax.functor._
    import com.wanari.tutelar.util.ApplicativeErrorSyntax._

    val requestF: F[HttpRequest] = configF().map { config =>
      val url         = s"${config.url}/send"
      val entity      = HttpEntity(ContentTypes.`application/json`, data.toJson.compactPrint)
      val credentials = BasicHttpCredentials(config.username, config.password)

      HttpRequest(POST, url, entity = entity)
        .addCredentials(credentials)
    }

    for {
      request  <- requestF
      response <- http.singleRequest(request)
      result   <- response.status.isSuccess().pureUnitOrRise(new Exception())
    } yield result

  }
}

object EmailServiceImpl {
  import spray.json.DefaultJsonProtocol._
  protected case class EmailRequestData(
      email: String,
      templateId: String,
      titleArguments: JsObject,
      bodyArguments: JsObject
  )
  protected implicit val emailRequestDataFormat: RootJsonFormat[EmailRequestData] = jsonFormat4(EmailRequestData)
}
