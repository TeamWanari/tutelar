package com.wanari.tutelar.core.impl.jwt

import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import spray.json.{JsObject, JsString}

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class JwtServiceSpec extends TestBase {

  def createServiceWithConf(config: JwtConfig): JwtService[Try] = {
    import cats.instances.try_._
    implicit val configService = () => Success(config)
    new JwtServiceImpl[Try]
  }

  val symmetric = Seq("HMD5", "HS224", "HS256", "HS384", "HS512")
    .map { algo =>
      val config1  = JwtConfig(1.day, algo, "secret1", "", "")
      val config2  = config1.copy(secret = "secret2")
      val service1 = createServiceWithConf(config1)
      val service2 = createServiceWithConf(config2)
      (algo, service1, service2)
    }

  val asymmetricRSA = Seq("RS256", "RS384", "RS512")
    .map { algo =>
      val config1  = JwtConfig(1.day, algo, "", JwtServiceSpec.privateRSAKey1, JwtServiceSpec.publicRSAKey1)
      val config2  = config1.copy(privateKey = JwtServiceSpec.privateRSAKey2)
      val service1 = createServiceWithConf(config1)
      val service2 = createServiceWithConf(config2)
      (algo, service1, service2)
    }

  val asymmetricES = Seq("ES256", "ES384", "ES512")
    .map { algo =>
      val config1  = JwtConfig(1.day, algo, "", JwtServiceSpec.privateECKey1, JwtServiceSpec.publicECKey1)
      val config2  = config1.copy(privateKey = JwtServiceSpec.privateECKey2)
      val service1 = createServiceWithConf(config1)
      val service2 = createServiceWithConf(config2)
      (algo, service1, service2)
    }

  val data = JsObject("hello" -> JsString("JWT"))

  (symmetric ++ asymmetricRSA ++ asymmetricES)
    .foreach {
      case (algo, service, service2) =>
        algo should {
          "encode-decode" in {
            (for {
              encoded <- service.encode(data)
              decoded <- service.decode(encoded)
            } yield {
              decoded.getFields("hello") shouldEqual Seq(JsString("JWT"))
            }).get
          }
          "decode fail - wrong secret" in {
            (for {
              encoded <- service2.encode(data)
            } yield {
              service.decode(encoded) shouldBe a[Failure[_]]
            }).get
          }
          "decode fail" in {
            service.decode("wrongtoken") shouldBe a[Failure[_]]
          }
          "validate" in {
            (for {
              encoded <- service.encode(data)
              result  <- service.validate(encoded)
            } yield {
              result shouldBe true
            }).get
          }
          "validate fail - wrong secret" in {
            (for {
              encoded <- service2.encode(data)
              result  <- service.validate(encoded)
            } yield {
              result shouldBe false
            }).get
          }
          "validate fail" in {
            service.validate("wrongtoken") shouldEqual Success(false)
          }
          "validateAndDecode" in {
            (for {
              encoded <- service.encode(data)
              decoded <- service.validateAndDecode(encoded)
            } yield {
              decoded.getFields("hello") shouldEqual Seq(JsString("JWT"))
            }).get
          }
          "validateAndDecode fail - wrong secret" in {
            (for {
              encoded <- service2.encode(data)
              result  <- service.validateAndDecode(encoded)
            } yield {
              result
            }) shouldBe a[Failure[_]]
          }
          "validateAndDecode fail" in {
            service.validateAndDecode("wrongtoken") shouldBe a[Failure[_]]
          }
        }
    }
}

object JwtServiceSpec {
  val privateRSAKey1 = """-----BEGIN RSA PRIVATE KEY-----
                         |MIIEowIBAAKCAQEAmQvOH62P7yhmckw1csTf17JQPtKSklHkAiNG6Gxdvf7ZKMzh
                         |bvnOzymYJigaGL6TLblR5v1C8nUchQhA1auCZ94xi7DIvl/XCJPjbyohCgoBOLDh
                         |g1R4V/AtPepEi/gaOvQKEKZX5HV08PIspPTWeIVTf0BxRZzf/Zz1Rc1nMMZX46zm
                         |B7LjJWNa6BHe0C9ZmjaivvhTFw9V7otShBXeObUrh3OK6IigZrT1z52HxBKWxiit
                         |OSuPWuJgDiq6vVkn/sH+yOas6DooU59a+phIJ+JaayM/NsTF1aDcJy6mIJAwsUKE
                         |MnKZ+ou+gATCzIW7/7ODrdE5cV/9vIibUrMT6wIDAQABAoIBAQCOVPlEYrCqdYMZ
                         |JyDJ9KhMPCv+0Oy5IWmQR2iJfUaNDPa+yBOblr0r0n4Kdl8Wxh2wd1nhHYXmYN2+
                         |JtfNHy0vFeg0Bpwa2JzuAEOSvbZuVLGgHHgOID+vYNFidH19QqZ6Tz0REPJKqKWx
                         |8zdrzbur6Cqn/LGbUPLLNO6yFP2z0EH/DcDRkeGq72h117cN3L7Lr5ve4PNzECpS
                         |xhKl5CdWdoQsF7KM0TICOx3X67A86sa14ML2Y4sFWYvgpj9nlh7c+1X/PfnJKEnU
                         |EjXAcnuk48c+wxf80bHDsCOaDE3src8mhy1J/3KKeeXPEFujf8SszTa9icYYPClM
                         |iFGNGfC5AoGBAMZS6YKf/IHup7EeCFBhF9SHmHXmr1WFGAtzJAi7FbqBGJ74NOsI
                         |CkUv4AApGz6FI9hM9cWrxVmrwGGCHdbxliF+UsUkWs0/kKySaNZWTmzE5tlIS1eC
                         |iaKeoR1jT0IhURfAO9FrCHwOcPjqoxkhy0VG7XGTpLiME+qrgCd7aeIVAoGBAMWN
                         |/fx9WnCZmzGbXRUof9i9U4SU3AoOsp2SQVmWKnw+Hui1yVjchoJ6+dZZok/uGEgd
                         |1EYfyJ2AcMxS9dN8tP+57j2EY7cSSPU6aRgIl+FPxeNNOw+gquuMK/bJfAnVvar/
                         |vg5cMBjIEaXQKKq2nylYzZUosR+J2CvHHV6LRJ3/AoGATgm6EMhbV9VM7wjeRKKv
                         |+dURTPNk8sXYXEkGWNklB6pcwdDxIbqcL/VSsz15lvRU0nwWCZ45nbtTjArjKv0N
                         |Ekje0OwpPrJQf1dtIUn8uhgQrlcgLmMTPYYl56Z4PZFWk331C8aOJCKamZfabb06
                         |exwZuqNaIbQc8i5h0ydg1rECgYBYGUxMzvIICNhGtQw7pUXuN/AIzgGakpdg4Zo3
                         |A1qK8YEDMh5KfH2XrpO/3VUe5AT8FCFX7FCgvGiRFeX+nDxzVk1CLcnyGDtk8Nlx
                         |GFPy7IpJJWXTQEk4pdftREkGccUVftsYuE/SnVYRZdTc3Hf6DloPzIfAks5OJ4uX
                         |X/AHrQKBgCJtX4m0ID3gV2u0fvpw6zasus5eNQe18i+j53kPeO/umfU/894eCLTY
                         |aOfK83aCtHnlOTukylDo7+hM4k3pNwivcALzKKN1R3+MXPswVs7dQfqNmsVBep/t
                         |g5bg7jJ4icreDHPNf1aQOCZzKZbezjO+Zja/oVS+A37OpjhjL/V1
                         |-----END RSA PRIVATE KEY-----
                         |""".stripMargin
  val publicRSAKey1  = """-----BEGIN PUBLIC KEY-----
                         |MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmQvOH62P7yhmckw1csTf
                         |17JQPtKSklHkAiNG6Gxdvf7ZKMzhbvnOzymYJigaGL6TLblR5v1C8nUchQhA1auC
                         |Z94xi7DIvl/XCJPjbyohCgoBOLDhg1R4V/AtPepEi/gaOvQKEKZX5HV08PIspPTW
                         |eIVTf0BxRZzf/Zz1Rc1nMMZX46zmB7LjJWNa6BHe0C9ZmjaivvhTFw9V7otShBXe
                         |ObUrh3OK6IigZrT1z52HxBKWxiitOSuPWuJgDiq6vVkn/sH+yOas6DooU59a+phI
                         |J+JaayM/NsTF1aDcJy6mIJAwsUKEMnKZ+ou+gATCzIW7/7ODrdE5cV/9vIibUrMT
                         |6wIDAQAB
                         |-----END PUBLIC KEY-----
                         |""".stripMargin
  val privateRSAKey2 = """-----BEGIN RSA PRIVATE KEY-----
                         |MIIEpAIBAAKCAQEA2gvq1grjy+BntlCkdFoHYug904n32Z9YSMdRdOvvclXdQTYU
                         |8iD5p27gx90qSzfUUpoJ4R8pc0Ky23/KJI8KOYUG1iMibcdkdMWl5PJynu+JUbqt
                         |q5Kd8uwl+cKkUy22qUQ7gTqP/WqMdvvUTWN/iEvTyzg/bIIYqhtzJY5MQN9LBQzo
                         |r5Fk2/tvRZDAe+NruBrUqte6qas9Dd+IRESfhHABgX0a0VEc+8hlDBEkPA3+jxER
                         |HojuefjK+zcnM1Arz2iUUrtUpOXB0PRMNIsRBRrl2lQ85fjKDXXR06XU33KlTLwS
                         |gGe3ZdglIUVTCjYIygk70RJvhNhwmrrB/AwwlwIDAQABAoIBAEQfMwgaMN8SRfSs
                         |ryR2uBYLjr1XPmrsII3kT6uixpVHBDAKcHLRII5R0sI+c6c6UwhXfbyqmq2a6fwv
                         |qXzQf5ZG3ELsiSYZBGaDFXg40tya46D4HKgcz2IEqzyjtekSwB2T5q9SF4hJ0Iaf
                         |2M0wx7hSUGIOOapx3rjOiKP0GBiv1sJSdE5Fi9Zo74ZYrX7lCdvcxW/v9TjbHXxR
                         |EaNDzwAjrW4SUkpe3YLJ/EsxWI7YFVeeh76Q7se/tN2JmoK1hWVDon/zonvLzbdI
                         |wyYw1AVPHQdvHTkaKWzm9iGOZ36vNxE6q8AF6BjzIX4A0pywh2XL6K8OvGAdGaiA
                         |vxmA1WECgYEA7tMXo96Ho4q6DrRSdUl9cPbQsRbnm/grdJmOsomGqHOghyidNc3m
                         |z4VKJ8gtmdE4dZ9uh/jOjkuvsA9FpPssk47vTpPNKdWJafpbQM1wmiCcBe6jyAj3
                         |FOXu3lBQea2bpy1MaXl6RkpYgA9rKqbi5utM87wH7jYn3GBd4Qf5X7ECgYEA6bpJ
                         |6BjrhXp0rZUnfXob87eRHXR848gDT5T6s7EliX1UFzaujZIiuV5X8QHAj229Q590
                         |VRiQ/HxGBZA6Rp0DZeedOL9w9/4Bih/cF85ty2yKJfGDj/RoVq43u0FcUd3F3djM
                         |FUUSbKIUICK3AQWgBmWTumkYA/dWZbi54aNRLscCgYEApqCXRNj07/DRgYLkaTqe
                         |D9vUnUnqzJJo11BwFDcJwavy23pFvY1sNWu84mazEje8Wayj9LBuhS9fY8o0ADjS
                         |0B0Q8FUE9uQqnMt1MZBkuNR5p1Xp5Z4DrgwDDg8hJtQu3oQnZQvBeRtUBf07+yU2
                         |+IBBT2joa5ZTV1nRBjmHDvECgYEAuWvophDfCA2V8v+ZTJpLApZmsY9wZOSQe9oc
                         |6eFnfiPHSoM/B2Ef2x9VdQWG1kKhG7ysdbX/j86nXlKFaO+3emi8+gAmhxcj8YhE
                         |+z3xLKj3EBMB5HppLBsaa5v2uZvPFaigf19Etpn+jV/8/vqPcYO0Jvao7ryR9jEO
                         |hksiZJECgYAFkORYAvvx9RVHKcL/H8/CPZ96PZPrTPTXOJdEILtOTBYXKI5pcgVG
                         |A8MEsvhik3ghlEwcFZ9fe49kx+BrYgiwN+4PubO6b7usV5MNhA3bxdsgwJxoUQMG
                         |S1JVm6xx05o+6pVOCyT+47zCAYVzgjiiAakGwHoTUEaR5V5St+IxWA==
                         |-----END RSA PRIVATE KEY-----
                         |""".stripMargin

  val privateECKey1 = """-----BEGIN EC PRIVATE KEY-----
                        |MIH3AgEAMBAGByqGSM49AgEGBSuBBAAjBIHfMIHcAgEBBEIACNGJ+TJtCh5DgmSH
                        |K7YYze4r0GFMFibOXSWpNxOp2qocaAu1305eu5lI6IAviVpdcWEt7fFea/0xB0Du
                        |a26mjjmgBwYFK4EEACOhgYkDgYYABAFLPfqcYr2+qgJg6P7VSdcz9haIHjI8j+al
                        |4naWezD4FlxfuuTt/diCmEu+uMt4NYaFl/MNWnnXFa0in6Bx7KwfJgBBNgGD9gsQ
                        |QY9OMjDNiHFcRqsSTjdf5tnoCv6p6/pNeGr6aqM59oGMmAtWANDhaSZ985YQbK+8
                        |u3Gdhy1jh3trLQ==
                        |-----END EC PRIVATE KEY-----
                        |""".stripMargin
  val publicECKey1  = """-----BEGIN PUBLIC KEY-----
                        |MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBSz36nGK9vqoCYOj+1UnXM/YWiB4y
                        |PI/mpeJ2lnsw+BZcX7rk7f3YgphLvrjLeDWGhZfzDVp51xWtIp+gceysHyYAQTYB
                        |g/YLEEGPTjIwzYhxXEarEk43X+bZ6Ar+qev6TXhq+mqjOfaBjJgLVgDQ4WkmffOW
                        |EGyvvLtxnYctY4d7ay0=
                        |-----END PUBLIC KEY-----
                        |""".stripMargin
  val privateECKey2 = """-----BEGIN EC PRIVATE KEY-----
                        |MIH3AgEAMBAGByqGSM49AgEGBSuBBAAjBIHfMIHcAgEBBEIBH3YxW5SXadNwa8yW
                        |nLesf8pek6jhNCAYfrVZQwljgFOqlomUojK4HTVMGSv8kpBy5fT8tA5MmDiHA4jE
                        |YLKoTiWgBwYFK4EEACOhgYkDgYYABADwflqa45ej15BybS2bA3u7FCchPD+AE9+P
                        |Ck/YQqfe/ZFRv5Z+81IlF1AiIhZZaGlOHbKnDkF5GGfEY5GR3tgoOQGRwlxxh+Oi
                        |7rIAy1y48CS71bTvNHfyZuGinRNo5Q04pYTsBvrgkKzjtK3vaowT/KUwB8rGrkcn
                        |fqAkCqt6gXz7rw==
                        |-----END EC PRIVATE KEY-----
                        |""".stripMargin

}
