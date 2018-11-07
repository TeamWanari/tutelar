package com.wanari.tutelar.providers.ldap

import java.util.Properties

import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.CallbackUrl
import javax.naming.Context
import javax.naming.directory.{Attributes, InitialDirContext, SearchControls, SearchResult}
import spray.json.{JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class LdapServiceImpl(
    implicit ec: ExecutionContext,
    config: LdapConfigService[Future],
    authService: AuthService[Future]
) extends LdapService[Future] {
  private lazy val context  = createSearchContext()
  private lazy val controls = createSearchControls()

  override def login(username: String, password: String): Future[CallbackUrl] = {
    for {
      user     <- findUser(username)
      _        <- getUserInitialDirContext(user.getNameInNamespace, password)
      callback <- authService.registerOrLogin("LDAP", username, "")
    } yield {
      val attributes = JsObject(attributesConvertToMap(user.getAttributes))
      callback
    }
  }

  private def attributesConvertToMap(attr: Attributes): Map[String, JsValue] = {
    import collection.JavaConverters._
    attr.getAll.asScala
      .map(e => e.getID -> convertToJsValue(e.get()))
      .toMap
  }

  private def convertToJsValue(value: Object): JsValue = {
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
    for {
      host <- config.getLdapUrl
      ctx <- Future {
        val props = new Properties
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        props.put(Context.PROVIDER_URL, host)
        props.put(Context.SECURITY_PRINCIPAL, username)
        props.put(Context.SECURITY_CREDENTIALS, password)

        new InitialDirContext(props)
      }
    } yield ctx
  }

  private def findUser(username: String): Future[SearchResult] = {
    for {
      ctx             <- context
      ctrl            <- controls
      baseDomain      <- config.getUserSearchBaseDomain
      searchAttribute <- config.getUserSearchAttribute
      data <- Future {
        val ans = ctx.search(baseDomain, s"$searchAttribute=$username", ctrl)
        ans.nextElement
      }
    } yield data
  }

  private def createSearchControls(): Future[SearchControls] = {
    for {
      attributes <- config.getUserSearchReturnAttributes
    } yield {
      val controls: SearchControls = new SearchControls
      controls.setReturningAttributes(attributes.toArray)
      controls.setSearchScope(SearchControls.SUBTREE_SCOPE)
      controls
    }
  }

  private def createSearchContext(): Future[InitialDirContext] = {
    for {
      user <- config.getReadonlyUserWithNameSpace
      pass <- config.getReadonlyUserPassword
      ctx  <- getUserInitialDirContext(user, pass)
    } yield ctx
  }
}
