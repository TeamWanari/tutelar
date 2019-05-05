package com.wanari.tutelar.core

import akka.http.scaladsl.model.HttpResponse

object Errors {
  sealed abstract class ConfigError(msg: String) extends Throwable(msg)
  case class WrongConfig(msg: String)            extends ConfigError(msg)

  sealed abstract class AuthError(msg: String) extends Throwable(msg)
  case class UserNotFound()                    extends AuthError(s"User not found")
  case class AccountNotFound()                 extends AuthError("Account not found")
  case class AccountUsed()                     extends AuthError("Account already used")
  case class UserHadThisAccountType()          extends AuthError("User already had this type of account")
  case class UserLastAccount()                 extends AuthError("This account is the users last account")
  case class AuthenticationFailed()            extends AuthError("Authentication failed, wrong credentials")

  sealed abstract class Oauth2Error(msg: String)       extends Throwable(msg)
  case class InvalidCsrfToken()                        extends Oauth2Error("Wrong csrf token")
  case class InvalidProfileDataMissingKey(key: String) extends Oauth2Error(s"Invalid profile data, missing key: $key")
  case class InvalidProfileDataNotJsonObject()         extends Oauth2Error("Invalid profile data, is not a JSON object")

  sealed abstract class JwtError(msg: String) extends Throwable(msg)
  case class InvalidJwt()                     extends JwtError("Invalid JWT")

  sealed abstract class UserPassError(msg: String) extends Throwable(msg)
  case class WeakPassword()                        extends UserPassError("The password is weak")
  case class UsernameUsed()                        extends UserPassError("Username already used")

  sealed abstract class EmailPassError(msg: String) extends UserPassError(msg)
  case class InvalidEmailToken()                    extends EmailPassError("Invalid email token")

  sealed abstract class EmailHttpServiceError(msg: String) extends EmailPassError(msg)
  case class HttpClientError(response: HttpResponse)
      extends EmailHttpServiceError(s"Error: StatusCode=${response.status}")

  sealed abstract class TotpError(msg: String) extends Throwable(msg)

}
