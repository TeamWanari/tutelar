package com.wanari.tutelar.providers.userpass.ldap
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.providers.userpass.UserPassService
import com.wanari.tutelar.providers.userpass.ldap.LdapService.LdapUserListData
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsValue

trait LdapService[F[_]] extends UserPassService[F] with Initable[F] {
  def listUsers()(implicit ctx: LogContext): ErrorOr[F, Seq[LdapUserListData]]
}

object LdapService {
  case class LdapUserListData(id: Option[String], ldapData: Map[String, JsValue])
}
