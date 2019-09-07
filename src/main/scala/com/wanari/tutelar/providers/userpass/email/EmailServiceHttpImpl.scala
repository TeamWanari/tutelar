package com.wanari.tutelar.providers.userpass.email

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import cats.MonadError
import com.wanari.tutelar.providers.userpass.email.EmailServiceHttpImpl.{EmailRequestData, EmailServiceHttpConfig}
import com.wanari.tutelar.util.HttpWrapper
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json._

class EmailServiceHttpImpl[F[_]: MonadError[*[_], Throwable]](
    implicit http: HttpWrapper[F],
    configF: () => F[EmailServiceHttpConfig]
) extends EmailService[F] {

  override def sendRegisterUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    send(EmailRequestData(email, "register", JsObject(), JsObject("token" -> JsString(token))))
  }

  override def sendResetPasswordUrl(email: String, token: String)(implicit ctx: LogContext): F[Unit] = {
    send(EmailRequestData(email, "reset-password", JsObject(), JsObject("token" -> JsString(token))))
  }

  private def send(data: EmailRequestData)(implicit ctx: LogContext): F[Unit] = {
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
      result   <- response.status.isSuccess().pureUnitOrRise(new Exception()) // TODO HttpClientError
    } yield result

  }
}

object EmailServiceHttpImpl {
  import spray.json.DefaultJsonProtocol._

  case class EmailServiceHttpConfig(
      url: String,
      username: String,
      password: String
  )

  protected case class EmailRequestData(
      email: String,
      templateId: String,
      titleArguments: JsObject,
      bodyArguments: JsObject
  )
  protected implicit val emailRequestDataFormat: RootJsonFormat[EmailRequestData] = jsonFormat4(EmailRequestData)
}
