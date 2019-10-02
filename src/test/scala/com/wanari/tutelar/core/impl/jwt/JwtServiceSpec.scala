package com.wanari.tutelar.core.impl.jwt

import cats.data.EitherT
import com.wanari.tutelar.TestBase
import com.wanari.tutelar.core.Errors.InvalidJwt
import com.wanari.tutelar.core.JwtService
import com.wanari.tutelar.core.impl.jwt.JwtServiceImpl.JwtConfig
import spray.json.{JsObject, JsString}

import scala.concurrent.duration._
import scala.util.{Success, Try}

class JwtServiceSpec extends TestBase {
  import cats.instances.try_._

  def createServiceWithConf(implicit config: JwtConfig): JwtService[Try] = {
    val service = new JwtServiceImpl[Try](config)
    service.init.get
    service
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
      val config2  = config1.copy(privateKey = JwtServiceSpec.privateRSAKey2, publicKey = JwtServiceSpec.publicRSAKey2)
      val service1 = createServiceWithConf(config1)
      val service2 = createServiceWithConf(config2)
      (algo, service1, service2)
    }

  val asymmetricES = Seq("ES256", "ES384", "ES512")
    .map { algo =>
      val config1  = JwtConfig(1.day, algo, "", JwtServiceSpec.privateECKey1, JwtServiceSpec.publicECKey1)
      val config2  = config1.copy(privateKey = JwtServiceSpec.privateECKey2, publicKey = JwtServiceSpec.publicECKey2)
      val service1 = createServiceWithConf(config1)
      val service2 = createServiceWithConf(config2)
      (algo, service1, service2)
    }

  val data = JsObject("hello" -> JsString("JWT"))

  (symmetric ++ asymmetricRSA ++ asymmetricES)
    .foreach {
      case (algo, service, service2) =>
        algo should {
          "encode-validateAndDecode" in {
            val result = for {
              encoded <- EitherT.right(service.encode(data))
              decoded <- service.validateAndDecode(encoded)
            } yield {
              decoded.getFields("hello")
            }
            result shouldEqual EitherT.rightT(Seq(JsString("JWT")))
          }
          "validateAndDecode fail - wrong secret" in {
            val result = for {
              encoded <- EitherT.right(service2.encode(data))
              res     <- service.validateAndDecode(encoded)
            } yield res
            result shouldBe EitherT.leftT(InvalidJwt())
          }
          "validateAndDecode fail" in {
            val result = service.validateAndDecode("wrongtoken")
            result shouldBe EitherT.leftT(InvalidJwt())
          }
          "validate" in {
            val result = for {
              encoded <- service.encode(data)
              result  <- service.validate(encoded)
            } yield result
            result shouldEqual Success(true)
          }
          "validate fail - wrong secret" in {
            val result = for {
              encoded <- service2.encode(data)
              result  <- service.validate(encoded)
            } yield result
            result shouldEqual Success(false)
          }
          "validate fail" in {
            service.validate("wrongtoken") shouldEqual Success(false)
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
                         |MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQClpZWHjKUtqyaP
                         |mdBSXMsJYqdZ/wa/E9EkREa/zcQxSAtFBGgRr8PbV75xlOKnNL0Y2sLT6/AGLBlI
                         |6wekcV4Vc5Sr0yZ9Kh/ZNG68Lyo0MxDv3pb08ckmxAuKhz4OgI0ax/IuWxqyLxTh
                         |7CdgCye0NrG+2FDWZuJqGLu29vZeYoNpXFfnvJYXC+FaK0hLQzLOBecpIFRcPML9
                         |PNpzjIoEZSvsB8x0SO6RTXXvKadAF2sTeuKBztcdDMgY4hjFUEdCPJVNt+c68VQQ
                         |agB3S1xMqF/Ao1PHIlr+EVgNF5+DXwyrZwzHm6af5WOEQFMBNOx3/pR9xHtoRaaL
                         |Iso/tdlHAgMBAAECggEBAKG7TogOdqhkVz4WPCRunX8IZ8WjDv85ZhY2460aRtin
                         |MvmsF8BNELn0relQKQyAnbDKxzcLQkuEexuK/uc8GVRwiVRK0WWb34S/gO8UTeyx
                         |f3P0rQdzm6bR+0LCUYDvWtYvKvK/2Qzok0cSwE8yFQ4L6PghVKxBwAc/Jui5sErt
                         |59OOkaLWhcJbjjJRB9sSfTmuPVsUHrBsTQoVuhMXHgiRSz4gk2bnP/QQERtD8Si/
                         |46w/34a3/ErEe/NV/eiNNeHGxqEX8itkXtCx9YoAbt0qEKdUHiK6XDAIJ7CLk+WL
                         |NPA8kkEY74PG6XJgGTtuC/WVC9H6U70C5A5IGYPtxkECgYEA595S/8zws5dccYHz
                         |4x7OYMTP8M9Is3W9v7Kwk6pffP+JfZKd7ByWT/lrBNNgvAC48BGVhjJkAt+vHF9z
                         |jXwYOlruC0+H24gYSDwbI86N2sLNZez+Wljeyn1lazZu/1QF5VdvXn4RTv7CXMQ+
                         |Iqv3UrQ+lg6Ih9Q2aaC2uEjInl8CgYEAtuLqV3FjVbNq8Rv9//AyFjVE6bgkXzCc
                         |LjOi99b7V05g7Dw1ddxWA8RbVv6LDLUAv83CjhG2MO8fxrz18MRRSxx7bQMTBOp/
                         |ASasIm6wghOCbIvyqb2KcjMKJuUELVfKmt80HTq5kJC1QcOSL3Rmvy5fuvAQ1SCW
                         |XO7atzo43hkCgYEAgrig6n5MJbPr9kJhkWZIy92prgXu4t46f9zqGBYxh3M5vIXw
                         |arEjPStM3oedPeDaYt5HAkVehRA+1SwrJVUVA7FICzBnU6lCp1bbpjBJYU/6JMCc
                         |FauMz3QqvWsO4Pwp5saIjylb8MFIKqyoqztwUDw2HLtM1ecaViq5WOQP1tkCgYB9
                         |IH2i/DIxhYLqmfNLs+Qg66tNmS5Rbmm89plOpmjqj/aiSoNtMyYqh6LSv28Vb5Wm
                         |pTmyiA22JzT/fXNrmnXgRQlxSUQu/d2NRQ9Ks57SMFTwvUN2vPbHMYKFn/UerM0y
                         |7vmx8ebaMRfCefM/wo01yp22wd9SYmxeAxHjgNM9qQKBgGRkoZ9VvKWuy/RYHT4n
                         |hwu+HVj/UqRI1b5hgcf1wLMibFq1gkzQoDTNGZ6NtNyCqMVodAXvtlJdgbItBUQf
                         |oYqYo1PpERSe2RXfnNDI8/5lokmmYwZIX5Q+ssu45duKihPyMdAqteyi0YYmMDQk
                         |PFM4Dl9nfirsZZzsmLRGXKxn
                         |-----END RSA PRIVATE KEY-----
                         |""".stripMargin
  val publicRSAKey2  = """-----BEGIN PUBLIC KEY-----
                         |MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApaWVh4ylLasmj5nQUlzL
                         |CWKnWf8GvxPRJERGv83EMUgLRQRoEa/D21e+cZTipzS9GNrC0+vwBiwZSOsHpHFe
                         |FXOUq9MmfSof2TRuvC8qNDMQ796W9PHJJsQLioc+DoCNGsfyLlsasi8U4ewnYAsn
                         |tDaxvthQ1mbiahi7tvb2XmKDaVxX57yWFwvhWitIS0MyzgXnKSBUXDzC/Tzac4yK
                         |BGUr7AfMdEjukU117ymnQBdrE3rigc7XHQzIGOIYxVBHQjyVTbfnOvFUEGoAd0tc
                         |TKhfwKNTxyJa/hFYDRefg18Mq2cMx5umn+VjhEBTATTsd/6UfcR7aEWmiyLKP7XZ
                         |RwIDAQAB
                         |-----END PUBLIC KEY-----
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
                        |MIH3AgEAMBAGByqGSM49AgEGBSuBBAAjBIHfMIHcAgEBBEIB0MDbtqqkzOG17rI8
                        |9GB9E7kAD8wiXypUc2mvt2E8RAXiIOfQhcJga/op6z5DESkOE9nbZr79ELqzIAlJ
                        |uie20L+gBwYFK4EEACOhgYkDgYYABAGyOCJp7h3Gw26PJilIORcVNFWQyDQ6iWaf
                        |I1mR5kIspv+MzP/pmnqSV+2cCLvmpNj7rQb3QCuMabCihJ+rz9xu/wHAORY3vmln
                        |ECge5nJS8gd9pHA6m6ihMTvHsf8MlOBGfWKZ0nG7mxm9Y3tt/GsDLU5soupRISAA
                        |tYtS/BdcniSagg==
                        |-----END EC PRIVATE KEY-----
                        |""".stripMargin
  val publicECKey2  = """-----BEGIN PUBLIC KEY-----
                        |MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBsjgiae4dxsNujyYpSDkXFTRVkMg0
                        |OolmnyNZkeZCLKb/jMz/6Zp6klftnAi75qTY+60G90ArjGmwooSfq8/cbv8BwDkW
                        |N75pZxAoHuZyUvIHfaRwOpuooTE7x7H/DJTgRn1imdJxu5sZvWN7bfxrAy1ObKLq
                        |USEgALWLUvwXXJ4kmoI=
                        |-----END PUBLIC KEY-----
                        |""".stripMargin
}
