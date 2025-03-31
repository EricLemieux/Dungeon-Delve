package com.lemieuxdev.elevenlabs.services

import com.lemieuxdev.elevenlabs.client.ElevenLabsClient
import com.lemieuxdev.elevenlabs.models.TextToSpeechRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service for text-to-speech functionality using Eleven Labs API
 */
class TextToSpeechService(private val client: ElevenLabsClient) {
    private val logger: Logger = LoggerFactory.getLogger(TextToSpeechService::class.java)
    
    /**
     * Converts text to speech using Eleven Labs API
     * 
     * @param text The text to convert to speech
     * @param voiceId The ID of the voice to use (defaults to "21m00Tcm4TlvDq8ikWAM")
     * @return The audio data as a ByteArray
     * @throws Exception if the API call fails
     */
    suspend fun generateSpeech(text: String, voiceId: String = "21m00Tcm4TlvDq8ikWAM"): ByteArray {
        logger.debug("Generating speech for text: $text")
        
        val request = TextToSpeechRequest(text = text)
        
        logger.debug("Sending request to Eleven Labs API")
        val response = client.httpClient.post("${ElevenLabsClient.BASE_URL}/text-to-speech/$voiceId") {
            contentType(ContentType.Application.Json)
            header("xi-api-key", client.getApiKey())
            setBody(request)
        }
        
        logger.debug("Received response from Eleven Labs API with status: ${response.status}")
        
        if (response.status.isSuccess()) {
            logger.debug("Successfully generated speech")
            return response.body()
        } else {
            logger.error("Failed to generate speech: ${response.bodyAsText()}")
            throw Exception("Failed to generate speech: ${response.status}")
        }
    }
}