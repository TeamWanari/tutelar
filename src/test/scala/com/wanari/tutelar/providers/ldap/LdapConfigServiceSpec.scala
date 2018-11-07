package com.wanari.tutelar.providers.ldap

import cats.Id
import com.typesafe.config.ConfigFactory
import com.wanari.tutelar.TestBase

class LdapConfigServiceSpec extends TestBase {

  val configRoot = ConfigFactory.parseString("""url = "ldap://1.2.3.4:389"
                                               |readonlyUserWithNamespace = "cn=readonly,dc=example,dc=com"
                                               |readonlyUserPassword = "readonlypw"
                                               |userSearchBaseDomain = "ou=peaple,dc=example,dc=com"
                                               |userSearchAttribute = "cn"
                                               |userSearchReturnAttributes = "cn,sn,email"
                                               |""".stripMargin)

  val service = new LdapConfigServiceImpl[Id](configRoot)

  "#getLdapUrl" in {
    service.getLdapUrl shouldEqual "ldap://1.2.3.4:389"
  }
  "#getReadonlyUserPassword" in {
    service.getReadonlyUserPassword shouldEqual "readonlypw"
  }
  "#getReadonlyUserWithNameSpace" in {
    service.getReadonlyUserWithNameSpace shouldEqual "cn=readonly,dc=example,dc=com"
  }
  "#getUserSearchAttribute" in {
    service.getUserSearchAttribute shouldEqual "cn"
  }
  "#getUserSearchBaseDomain" in {
    service.getUserSearchBaseDomain shouldEqual "ou=peaple,dc=example,dc=com"
  }
  "#getUserSearchReturnAttributes" in {
    service.getUserSearchReturnAttributes shouldEqual Seq("cn", "sn", "email")
  }

}
