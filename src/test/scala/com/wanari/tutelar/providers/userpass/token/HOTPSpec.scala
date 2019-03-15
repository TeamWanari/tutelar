package com.wanari.tutelar.providers.userpass.token

import com.wanari.tutelar.providers.userpass.token.OTP._
import org.apache.commons.codec.binary.Hex
import org.scalatest.{Matchers, WordSpecLike}

class HOTPSpec extends WordSpecLike with Matchers {

  def fromHex(h: String) = OTPKey(new javax.crypto.spec.SecretKeySpec((new Hex).decode(h.getBytes), "RAW"))

  val otpkey = fromHex("3132333435363738393031323334353637383930")

  val hotp = HOTP(OTPAlgorithm.SHA1, 6, otpkey)

  // https://tools.ietf.org/html/rfc4226#page-32
  "HOTP" should {
    "generate a HOTP code (Appendix D - HOTP Algorithm: Test Values)" in {
      hotp.generate(0L) should ===("755224")
      hotp.generate(1L) should ===("287082")
      hotp.generate(2L) should ===("359152")
      hotp.generate(3L) should ===("969429")
      hotp.generate(4L) should ===("338314")
      hotp.generate(5L) should ===("254676")
      hotp.generate(6L) should ===("287922")
      hotp.generate(7L) should ===("162583")
      hotp.generate(8L) should ===("399871")
      hotp.generate(9L) should ===("520489")
    }

    "generate HOTP codes with window" in {
      hotp.generate(4L, 3) should ===(Map(4L -> "338314", 5L -> "254676", 6L -> "287922", 7L -> "162583"))
    }

    "validate the HOTP code (Appendix D - HOTP Algorithm: Test Values)" in {
      hotp.validate(0L)("000000") should be(false)
      hotp.validate(0L)("123456") should be(false)
      hotp.validate(Long.MaxValue)("123456") should be(false)
      hotp.validate(0L)("755224") should be(true)
      hotp.validate(1L)("287082") should be(true)
      hotp.validate(2L)("359152") should be(true)
      hotp.validate(3L)("969429") should be(true)
      hotp.validate(4L)("338314") should be(true)
      hotp.validate(5L)("254676") should be(true)
      hotp.validate(6L)("287922") should be(true)
      hotp.validate(7L)("162583") should be(true)
      hotp.validate(8L)("399871") should be(true)
      hotp.validate(9L)("520489") should be(true)
    }

    "validate the HOTP code with window and returns the gap" in {
      hotp.validate(0L, 0)("755224") should be(Some(0))
      hotp.validate(0L, 1)("287082") should be(Some(1))
      hotp.validate(0L, 2)("359152") should be(Some(2))
      hotp.validate(0L, 3)("969429") should be(Some(3))
      hotp.validate(0L, 4)("338314") should be(Some(4))
      hotp.validate(0L, 5)("254676") should be(Some(5))
      hotp.validate(0L, 6)("287922") should be(Some(6))
      hotp.validate(0L, 7)("162583") should be(Some(7))
      hotp.validate(0L, 8)("399871") should be(Some(8))
      hotp.validate(0L, 9)("520489") should be(Some(9))
      hotp.validate(3L, 10)("254676") should be(Some(2))
      hotp.validate(0L, 8)("520489") should be(None)
    }
  }
}
