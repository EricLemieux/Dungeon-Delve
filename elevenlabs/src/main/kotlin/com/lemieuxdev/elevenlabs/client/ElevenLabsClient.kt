package com.lemieuxdev.elevenlabs.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Client for interacting with the Eleven Labs API */
class ElevenLabsClient(private val apiKey: String) {
  private val logger: Logger = LoggerFactory.getLogger(ElevenLabsClient::class.java)

  val httpClient =
      HttpClient(CIO) {
        install(ContentNegotiation) {
          json(
              Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
              })
        }
        install(Logging) { level = LogLevel.INFO }
      }

  /** Get the API key for authentication with Eleven Labs API */
  fun getApiKey(): String {
    return apiKey
  }

  companion object {
    const val BASE_URL = "https://api.elevenlabs.io/v1"
  }
}
