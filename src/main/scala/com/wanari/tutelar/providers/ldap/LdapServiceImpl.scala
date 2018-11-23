package com.wanari.tutelar.providers.ldap

import java.util.Properties

import com.wanari.tutelar.core.AuthService
import com.wanari.tutelar.core.AuthService.CallbackUrl
import com.wanari.tutelar.providers.ldap.LdapServiceImpl.LdapConfig
import javax.naming.Context
import javax.naming.directory.{Attributes, InitialDirContext, SearchControls, SearchResult}
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class LdapServiceImpl(
    implicit ec: ExecutionContext,
    ldapConfig: () => Future[LdapConfig],
    authService: AuthService[Future]
) extends LdapService[Future] {
  private lazy val context  = createSearchContext()
  private lazy val controls = createSearchControls()

  override def login(username: String, password: String): Future[CallbackUrl] = {
    for {
      user       <- findUser(username)
      _          <- getUserInitialDirContext(user.getNameInNamespace, password)
      attributes <- attributesConvertToMap(user.getAttributes)
      callback <- authService.registerOrLogin("LDAP", username, "", JsObject(attributes))
    } yield {
      callback
    }
  }

  private def attributesConvertToMap(attributes: Attributes): Future[Map[String, JsValue]] = {
    ldapConfig().map { config =>
      import collection.JavaConverters._
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
    for {
      config <- ldapConfig()
      ctx <- Future {
        val props = new Properties
        props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        props.put(Context.PROVIDER_URL, config.ldapUrl)
        props.put(Context.SECURITY_PRINCIPAL, username)
        props.put(Context.SECURITY_CREDENTIALS, password)

        new InitialDirContext(props)
      }
    } yield ctx
  }

  private def findUser(username: String): Future[SearchResult] = {
    for {
      config <- ldapConfig()
      ctx    <- context
      ctrl   <- controls
      data <- Future {
        val ans = ctx.search(config.userSearchBaseDomain, s"${config.userSearchAttribute}=$username", ctrl)
        ans.nextElement
      }
    } yield data
  }

  private def createSearchControls(): Future[SearchControls] = {
    for {
      config <- ldapConfig()
    } yield {
      val returningAttributes      = config.userSearchReturnAttributes ++ config.userSearchReturnArrayAttributes
      val controls: SearchControls = new SearchControls
      controls.setReturningAttributes(returningAttributes.toArray)
      controls.setSearchScope(SearchControls.SUBTREE_SCOPE)
      controls
    }
  }

  private def createSearchContext(): Future[InitialDirContext] = {
    for {
      config <- ldapConfig()
      ctx    <- getUserInitialDirContext(config.readonlyUserWithNameSpace, config.readonlyUserPassword)
    } yield ctx
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
