package com.wanari.tutelar.providers.userpass.token

import java.security.{Key, SecureRandom}

import org.apache.commons.codec.binary.Base32

import scala.util.Try

// Most of the implementations came from https://github.com/ejisan/kuro-otp
// This is a highly refactored, minimalized version of the kuro-otp.
object OTP {

  trait OTPBase {
    def algorithm: OTPAlgorithm
    def digits: Int
    def otpkey: OTPKey
  }

  case class HOTP(algorithm: OTPAlgorithm, digits: Int, otpkey: OTPKey) extends OTPBase {
    import Calculations._

    def generate(counter: Long): String = {
      intToDigits(generateForCounter(this, counter), digits)
    }

    def generate(counter: Long, lookAheadWindow: Int): Map[Long, String] = {
      generateForCounter(this, aheadRange(counter, lookAheadWindow)).view.mapValues(intToDigits(_, digits)).toMap
    }

    def validate(counter: Long)(code: String): Boolean =
      validateWithCounter(this, counter, digitsToInt(code))

    def validate(counter: Long, lookAheadWindow: Int)(code: String): Option[Long] =
      validateWithCounter(this, counter, aheadRange(counter, lookAheadWindow), digitsToInt(code))

    def toURI(account: String, issuer: Option[String] = None, params: Map[String, String] = Map()): String = {
      generateURI(
        "hotp",
        account,
        otpkey,
        issuer,
        params ++ Map("digits" -> digits.toString, "algorithm" -> algorithm.name)
      )
    }
  }

  //period and other ts-es should be in the same magnitude (s, or ms)
  case class TOTP(algorithm: OTPAlgorithm, digits: Int, period: Int, initialTimestamp: Long, otpkey: OTPKey)
      extends OTPBase {
    import Calculations._

    def generate(timestamp: Long): String = {
      intToDigits(
        generateForCounter(this, calculateTimeCounter(period, timestamp, initialTimestamp)),
        digits
      )
    }

    def generate(timestamp: Long, window: Int): Map[Long, String] = {
      generateForCounter(
        this,
        symmetricRange(calculateTimeCounter(period, timestamp, initialTimestamp), window)
      ).view.mapValues(intToDigits(_, digits)).toMap
    }

    def validate(timestamp: Long)(code: String): Boolean =
      validateWithCounter(
        this,
        calculateTimeCounter(period, timestamp, initialTimestamp),
        digitsToInt(code)
      )

    def validate(timestamp: Long, window: Int)(code: String): Option[Long] = {
      val counter = calculateTimeCounter(period, timestamp, initialTimestamp)
      validateWithCounter(
        this,
        counter,
        symmetricRange(counter, window),
        digitsToInt(code)
      )
    }

    def toURI(account: String, issuer: Option[String] = None, params: Map[String, String] = Map()): String = {
      generateURI(
        "totp",
        account,
        otpkey,
        issuer,
        params ++ Map("digits" -> digits.toString, "algorithm" -> algorithm.name, "period" -> period.toString)
      )
    }
  }

  case class OTPKey private (key: Key) {
    require(
      key.getEncoded.length >= 16,
      "RFC 4226 requires key length of at least 128 bits and recommends key length of 160 bits."
    )
    require(key.getFormat.toUpperCase == "RAW", "Invalid Key format!")

    private def toByteArray: Array[Byte] = key.getEncoded

    def toBase32: String =
      (new Base32).encodeToString(toByteArray)

    override def toString: String = s"OtpKey($toBase32)"
  }

  object OTPKey {
    def apply(base32: String): OTPKey =
      OTPKey(new javax.crypto.spec.SecretKeySpec((new Base32).decode(base32), "RAW"))

    def defaultPRNG: SecureRandom =
      SecureRandom.getInstance("NativePRNGNonBlocking", "SUN")

    def randomStrong(algorithm: OTPAlgorithm, prng: SecureRandom = defaultPRNG): OTPKey = {
      val gen = javax.crypto.KeyGenerator.getInstance(algorithm.value)
      gen.init(algorithm.strongKeyLength, prng)
      OTPKey(gen.generateKey)
    }
  }

  case class OTPAlgorithm(
      name: String,
      value: String,
      strongKeyLength: Int
  )

  object OTPAlgorithm {
    val MD5    = OTPAlgorithm("MD5", "HmacMD5", 160)
    val SHA1   = OTPAlgorithm("SHA1", "HmacSHA1", 200)
    val SHA256 = OTPAlgorithm("SHA256", "HmacSHA256", 280)
    val SHA512 = OTPAlgorithm("SHA512", "HmacSHA512", 520)

    val algos = Seq(MD5, SHA1, SHA256, SHA512)
  }

  private object Calculations {
    def hmac(algorithm: OTPAlgorithm, otpkey: OTPKey, input: Array[Byte]): Array[Byte] = {
      val mac = javax.crypto.Mac.getInstance(algorithm.value, "SunJCE")
      mac.init(otpkey.key)
      mac.doFinal(input)
    }

    def truncateHmac(hmac: Array[Byte], digits: Int): Int = {
      val offset = hmac(hmac.length - 1) & 0x0f
      val truncatedHash: Long =
        ((hmac(offset) & 0x7f) << 24) |
          ((hmac(offset + 1) & 0xff) << 16) |
          ((hmac(offset + 2) & 0xff) << 8) |
          (hmac(offset + 3) & 0xff).toLong
      (truncatedHash % scala.math.pow(10, digits.toDouble)).toInt
    }

    def generateForCounter(base: OTPBase, counter: Long): Int = {
      val buffer = java.nio.ByteBuffer.allocate(8)
      buffer.putLong(0, counter)
      truncateHmac(hmac(base.algorithm, base.otpkey, buffer.array), base.digits)
    }

    def generateForCounter(
        base: OTPBase,
        range: Seq[Long]
    ): Map[Long, Int] = {
      range
        .map(c => c -> generateForCounter(base, c))
        .toMap
    }
    def symmetricRange(counter: Long, window: Int): Seq[Long] = (counter - window) to (counter + window)
    def aheadRange(counter: Long, window: Int): Seq[Long]     = counter to (counter + window)

    def validateWithCounter(
        base: OTPBase,
        counter: Long,
        code: Option[Int]
    ): Boolean =
      code.contains(generateForCounter(base, counter))

    def validateWithCounter(
        base: OTPBase,
        counter: Long,
        range: Seq[Long],
        code: Option[Int]
    ): Option[Long] = {
      code.flatMap { code =>
        generateForCounter(base, range)
          .find(_._2 == code)
          .map(_._1 - counter)
      }
    }

    def intToDigits(code: Int, digits: Int): String = {
      val s = code.toString
      "0" * (digits - s.length) + s
    }

    def digitsToInt(str: String): Option[Int] = Try(str.toInt).toOption

    def calculateTimeCounter(period: Int, instantTimestamp: Long, initialTimestamp: Long = 0): Long =
      (instantTimestamp - initialTimestamp) / period
  }

  private def generateURI(
      protocol: String,
      account: String,
      otpkey: OTPKey,
      issuer: Option[String],
      params: Map[String, String]
  ): String = {
    val label            = issuer.map(i => s"$i:$account").getOrElse(s"$account")
    val parameters       = params + ("secret" -> otpkey.toBase32) ++ issuer.map(i => Set("issuer" -> i)).getOrElse(Set())
    val parametersString = parameters.map(p => p._1 + "=" + p._2).mkString("&")
    s"otpauth://$protocol/$label?$parametersString"
  }
}
