package com.wanari.tutelar

import java.io.PrintStream
import java.util.logging.Level

import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.{Logger, LoggerFactory}

object LoggerUtil {
  def initBridge(): Unit = {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    java.util.logging.Logger.getLogger("").setLevel(Level.FINEST)
    System.setErr(new PrintStream((b: Int) => {}))
  }

  def getDefaultLogger: Logger = {
    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
  }

  def getLogger(name: String): Logger = {
    LoggerFactory.getLogger(name)
  }
}
