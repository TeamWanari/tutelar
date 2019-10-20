package com.wanari.tutelar.providers.userpass.ldap

import java.util.Properties

import cats.data.EitherT
import com.wanari.tutelar.core.AuthService.{LongTermToken, TokenData}
import com.wanari.tutelar.core.DatabaseService.UserIdWithExternalId
import com.wanari.tutelar.core.Errors.{AuthenticationFailed, ErrorOr}
import com.wanari.tutelar.core.{AuthService, DatabaseService}
import com.wanari.tutelar.providers.userpass.ldap.LdapService.LdapUserListData
import com.wanari.tutelar.providers.userpass.ldap.LdapServiceImpl.LdapConfig
import com.wanari.tutelar.util.LoggerUtil.LogContext
import javax.naming.Context
import javax.naming.directory.{Attributes, InitialDirContext, SearchControls, SearchResult}
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class LdapServiceImpl(
    implicit ec: ExecutionContext,
    config: LdapConfig,
    authService: AuthService[Future],
    databaseService: DatabaseService[Future]
) extends LdapService[Future] {
  import cats.instances.future._

  private val authType     = "LDAP"
  private lazy val context = getUserInitialDirContext(config.readonlyUserWithNameSpace, config.readonlyUserPassword)

  override def init: Future[Unit] = {
    context.map(_ => ())
  }

  override def login(username: String, password: String, data: Option[JsObject], refreshToken: Option[LongTermToken])(
      implicit ctx: LogContext
  ): ErrorOr[Future, TokenData] = {
    for {
      _          <- validateUserName(username)
      attributes <- EitherT(loginAndGetAttributes(username, password))
      token      <- authService.authenticatedWith("LDAP", username, "", JsObject(attributes), refreshToken)
    } yield token
  }

  private def validateUserName(username: String): ErrorOr[Future, Unit] = {
    val invalidChars = Seq('*', '"', '\'', ',', '=')
    val isValid      = !invalidChars.exists(username.contains(_))
    EitherT.cond(isValid, (), AuthenticationFailed())
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

  override def listUsers()(implicit ctx: LogContext): ErrorOr[Future, Seq[LdapUserListData]] = {
    val registeredLdapUsersF = databaseService.listUserIdsByAuthType(authType)
    val result = for {
      ctx <- context
      userIterator <- Future {
        ctx.search(config.userSearchBaseDomain, s"${config.userSearchAttribute}=*", controls)
      }
      ldapIdsFromDb <- registeredLdapUsersF
    } yield {
      import scala.jdk.CollectionConverters._
      userIterator.asScala.toSeq
        .map(_.getAttributes)
        .map(attributesConvertToMap)
        .map { ldapData =>
          val userIdOpt = findUserId(ldapIdsFromDb, ldapData)
          LdapUserListData(userIdOpt, ldapData)
        }
    }
    EitherT.right(result)
  }

  private def findUserId(ldapIdsFromDb: Seq[UserIdWithExternalId], ldapData: Map[String, JsValue]): Option[String] = {
    for {
      externalId <- ldapData.get(config.userSearchAttribute).collect { case JsString(id) => id }
      userId     <- ldapIdsFromDb.find(_.externalId == externalId).map(_.userId)
    } yield userId
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
