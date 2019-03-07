package com.wanari.tutelar.providers.userpass.ldap
import com.wanari.tutelar.providers.userpass.UserPassService

trait LdapService[F[_]] extends UserPassService[F]
