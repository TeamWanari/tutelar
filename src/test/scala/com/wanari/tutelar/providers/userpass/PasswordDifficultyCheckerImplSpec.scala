package com.wanari.tutelar.providers.userpass

import com.wanari.tutelar.providers.userpass.PasswordDifficultyCheckerImpl.PasswordSettings
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}

class PasswordDifficultyCheckerImplSpec extends AnyWordSpecLike with Matchers {
  trait TestScope {
    lazy val patternConfig                     = ""
    implicit lazy val config: PasswordSettings = PasswordSettings(patternConfig)
    lazy val service                           = new PasswordDifficultyCheckerImpl[Try]
  }

  val exactlyOneLowerChar      = "^[a-z]$"
  val twoOrThreeLowerChar      = "^[a-z]{2,3}$"
  val min8CharUpperLowerNumber = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$"

  "PasswordDifficultyCheckerImpl" should {
    Seq[(String, Seq[String], Seq[String])](
      (exactlyOneLowerChar, Seq("a"), Seq("", "A", "1", "aa")),
      (twoOrThreeLowerChar, Seq("aa", "bbb"), Seq("a", "AB", "12", "AAA", "aaaa")),
      (min8CharUpperLowerNumber, Seq("aaaBBB11", "aaaBBB1111"), Seq("aaabbb11", "aaaBBB1", "AAABBB11", "AAABBBAB"))
    ).foreach {
      case (pattern, validExamples, invalidExamples) =>
        new TestScope {
          override lazy val patternConfig: String = pattern
          s"validate with pattern - $pattern" should {
            validExamples.foreach { in =>
              s"valid - $in" in {
                service.isValid(in) shouldBe Success(true)
              }
            }
            invalidExamples.foreach { in =>
              s"invalid - $in" in {
                service.isValid(in) shouldBe Success(false)
              }
            }
          }
        }
    }
  }
}
