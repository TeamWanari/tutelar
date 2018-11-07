package com.wanari.tutelar.core.impl.jwt

object JwtKeyGenerator {
  import java.security.{KeyPair, KeyPairGenerator}
  import java.util.Base64

  def generateECDSAKeyPair: (String, String) = {
    import java.security.spec.ECGenParameterSpec
    import java.security.{SecureRandom, Security}

    import org.bouncycastle.jce.provider.BouncyCastleProvider

    val ecGenSpec = new ECGenParameterSpec("P-521")
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider())
    }

    val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
    keyGen.initialize(ecGenSpec, new SecureRandom())
    val ecKey = keyGen.generateKeyPair()

    convertToPem("EC", ecKey)
  }

  def generateRSAKeyPair: (String, String) = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(512)
    val rsaKey = keyGen.genKeyPair

    convertToPem("RSA", rsaKey)
  }

  private def convertToPem(keyName: String, keyPair: KeyPair): (String, String) = {
    val privateKey =
      s"-----BEGIN $keyName PRIVATE KEY-----\r\n" +
        convertToPemDataFormat(keyPair.getPrivate.getEncoded) +
        s"-----END $keyName PRIVATE KEY-----\r\n"
    val publicKey =
      "-----BEGIN PUBLIC KEY-----\r\n" +
        convertToPemDataFormat(keyPair.getPublic.getEncoded) +
        "-----END PUBLIC KEY-----\r\n"

    (privateKey, publicKey)
  }

  private def convertToPemDataFormat(data: Array[Byte]): String = {
    Base64.getEncoder.encodeToString(data).grouped(64).mkString("\r\n") + "\r\n"
  }
}
