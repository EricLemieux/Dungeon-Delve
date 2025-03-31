package com.lemieuxdev

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.html.*
import org.slf4j.LoggerFactory
import org.slf4j.event.*

fun Application.configureMonitoring() {
  val logger = LoggerFactory.getLogger("com.lemieuxdev.Monitoring")
  logger.debug("Configuring monitoring")

  install(CallLogging) {
    level = Level.INFO
    logger.debug("Setting call logging level to INFO")

    filter { call -> 
      val path = call.request.path()
      val shouldLog = path.startsWith("/")
      logger.debug("Call logging filter for path: $path, will log: $shouldLog")
      shouldLog
    }
  }

  logger.debug("Monitoring configured")
}
