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
import org.slf4j.event.*

fun Application.configureMonitoring() {
  install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
  }
}
