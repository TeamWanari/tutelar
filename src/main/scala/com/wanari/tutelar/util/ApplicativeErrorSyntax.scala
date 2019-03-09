package com.wanari.tutelar.util

import cats.data.OptionT
import cats.{Applicative, ApplicativeError, MonadError}
import spray.json.{JsValue, JsonReader}

import scala.util.Try

object ApplicativeErrorSyntax {
  implicit class ErrorOps(private val t: Throwable) extends AnyVal {
    def raise[F[_], A](implicit F: ApplicativeError[F, Throwable]) = F.raiseError[A](t)
  }
  implicit class OptionErrorOps[A](private val option: Option[A]) extends AnyVal {
    def pureOrRaise[F[_]: ApplicativeError[?[_], Throwable]](t: => Throwable) =
      option.fold(t.raise[F, A])(Applicative[F].pure(_))
  }
  implicit class OptionTErrorOps[F[_], A](private val opt: OptionT[F, A]) extends AnyVal {
    import cats.syntax.flatMap._
    def pureOrRaise(t: => Throwable)(implicit ev: MonadError[F, Throwable]) =
      opt.value.flatMap(_.pureOrRaise(t))
  }
  implicit class TryErrorOps[A](private val tri: Try[A]) extends AnyVal {
    def pureOrRise[F[_]: ApplicativeError[?[_], Throwable]] = {
      tri.fold(_.raise[F, A], Applicative[F].pure(_))
    }
  }
  implicit class BoolErrorOps(b: Boolean) {
    def pureUnitOrRise[F[_]: ApplicativeError[?[_], Throwable]](t: => Throwable): F[Unit] = {
      if (b) {
        Applicative[F].pure({})
      } else {
        t.raise[F, Unit]
      }
    }
  }

  implicit class JsonConvertToOps(private val jsval: JsValue) extends AnyVal {
    def convertToF[F[_]: ApplicativeError[?[_], Throwable], A: JsonReader]: F[A] = {
      convertToF(implicitly[JsonReader[A]])
    }
    def convertToF[F[_], A](reader: JsonReader[A])(implicit ae: ApplicativeError[F, Throwable]): F[A] = {
      Try(jsval.convertTo[A](reader)).pureOrRise
    }
  }
}
