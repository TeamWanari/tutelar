package com.wanari.tutelar.util

import org.mindrot.jbcrypt.BCrypt

trait PasswordCryptor {
  protected def encryptPassword(password: String): String              = BCrypt.hashpw(password, BCrypt.gensalt())
  protected def checkPassword(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}
