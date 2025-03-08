package com.lemieuxdev

import io.ktor.server.application.*

fun main(args: Array<String>) {
  io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
  configureMonitoring()
  configureTemplating()
}
