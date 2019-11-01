package com.wanari.tutelar.providers.userpass.ldap

import java.util.Properties

import cats.data.EitherT
import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.TokenData
import com.wanari.tutelar.core.Errors.{AuthenticationFailed, ErrorOr}
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import javax.naming.Context
import javax.naming.directory.{Attributes, InitialDirContext, SearchControls, SearchResult}
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class LdapServiceImpl(
    implicit ec: ExecutionContext,
    config: LdapConfig,
    authService: AuthService[Future]
) extends LdapService[Future] {
  private lazy val context = getUserInitialDirContext(config.readonlyUserWithNameSpace, config.readonlyUserPassword)

  import cats.instances.future._

  override def init: Future[Unit] = {
    context.map(_ => ())
  }

  override def login(username: String, password: String, data: Option[JsObject])(
      implicit ctx: LogContext
  ): ErrorOr[Future, TokenData] = {
    for {
      attributes <- EitherT(loginAndGetAttributes(username, password))
      token      <- authService.registerOrLogin("LDAP", username, "", JsObject(attributes))
    } yield token
  }

  private def loginAndGetAttributes(username: String, password: String) = {
    val result = for {
      user <- findUser(username)
      _    <- getUserInitialDirContext(user.getNameInNamespace, password)
    } yield {
      val attributes = attributesConvertToMap(user.getAttributes)
      Right(attributes)
    }
    result.recover {
      case _ => Left(AuthenticationFailed())
    }
  }

  private def attributesConvertToMap(attributes: Attributes): Map[String, JsValue] = {
    import scala.jdk.CollectionConverters._
    val arrayAttributes = config.userSearchReturnArrayAttributes.map(_.toLowerCase)
    attributes.getAll.asScala.map { attribute =>
      val jsValue = if (arrayAttributes.contains(attribute.getID.toLowerCase)) {
        JsArray(attribute.getAll.asScala.map(convertToJsValue).toVector)
      } else {
        convertToJsValue(attribute.get())
      }
      attribute.getID -> jsValue
    }.toMap
  }

  private def convertToJsValue(value: Any): JsValue = {
    value match {
      case x: java.lang.String  => JsString(x)
      case x: java.lang.Integer => JsNumber(x)
      case x: java.lang.Long    => JsNumber(x)
      case x: java.lang.Double  => JsNumber(x)
      case x: java.lang.Float   => JsNumber(x.toDouble)
      case x: java.lang.Boolean => JsBoolean(x)
      case x: java.lang.Byte    => JsNumber(x.toInt)
      case null                 => JsNull
      case x                    => JsString(x.toString)
    }
  }

  private def getUserInitialDirContext(username: String, password: String): Future[InitialDirContext] = {
    Future {
      val props = new Properties
      props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      props.put(Context.PROVIDER_URL, config.ldapUrl)
      props.put(Context.SECURITY_PRINCIPAL, username)
      props.put(Context.SECURITY_CREDENTIALS, password)

      new InitialDirContext(props)
    }
  }

  private def findUser(username: String): Future[SearchResult] = {
    for {
      ctx <- context
      data <- Future {
        val ans = ctx.search(config.userSearchBaseDomain, s"${config.userSearchAttribute}=$username", controls)
        ans.nextElement
      }
    } yield data
  }

  private lazy val controls: SearchControls = {
    val returningAttributes      = config.userSearchReturnAttributes ++ config.userSearchReturnArrayAttributes
    val controls: SearchControls = new SearchControls
    controls.setReturningAttributes(returningAttributes.toArray)
    controls.setSearchScope(SearchControls.SUBTREE_SCOPE)
    controls
  }
}

object LdapServiceImpl {
  case class LdapConfig(
      ldapUrl: String,
      readonlyUserWithNameSpace: String,
      readonlyUserPassword: String,
      userSearchBaseDomain: String,
      userSearchAttribute: String,
      userSearchReturnAttributes: Seq[String],
      userSearchReturnArrayAttributes: Seq[String]
  )
}
