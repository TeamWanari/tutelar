package com.wanari.tutelar.core

import cats.data.OptionT
import com.wanari.tutelar.Initable
import com.wanari.tutelar.core.AuthService.{LongTermToken, ShortTermToken, TokenData}
import com.wanari.tutelar.core.DatabaseService.Account
import com.wanari.tutelar.core.Errors.ErrorOr
import com.wanari.tutelar.util.LoggerUtil.LogContext
import spray.json.JsObject

trait AuthService[F[_]] extends Initable[F] {
  def findCustomData(authType: String, externalId: String): OptionT[F, String]
  def authenticatedWith(
      authType: String,
      externalId: String,
      customData: String,
      providedData: JsObject,
      refreshToken: Option[LongTermToken]
  )(implicit
      ctx: LogContext
  ): ErrorOr[F, TokenData]
  def deleteUser(userId: String)(implicit ctx: LogContext): ErrorOr[F, Unit]
  def findUserIdInShortTermToken(token: ShortTermToken): OptionT[F, String]
  def link(userId: String, authType: String, externalId: String, customData: String, providedData: JsObject)(implicit
      ctx: LogContext
  ): ErrorOr[F, (Account, JsObject)]
  def unlink(userId: String, authType: String)(implicit ctx: LogContext): ErrorOr[F, Unit]
  def refreshToken(token: LongTermToken)(implicit ctx: LogContext): ErrorOr[F, TokenData]
  def findProviderCustomDataByUserId(userId: String, authType: String)(implicit ctx: LogContext): OptionT[F, String]
}

object AuthService {
  case class TokenData(token: ShortTermToken, refreshToken: LongTermToken)
  type LongTermToken  = String
  type ShortTermToken = String
}
