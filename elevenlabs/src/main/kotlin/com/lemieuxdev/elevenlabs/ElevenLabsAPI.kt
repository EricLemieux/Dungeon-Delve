package com.lemieuxdev.elevenlabs

import com.lemieuxdev.elevenlabs.client.ElevenLabsClient
import com.lemieuxdev.elevenlabs.models.SpeechToTextResponse
import com.lemieuxdev.elevenlabs.services.SpeechToTextService
import com.lemieuxdev.elevenlabs.services.TextToSpeechService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Main API class for interacting with Eleven Labs API */
class ElevenLabsAPI(apiKey: String) {
  private val logger: Logger = LoggerFactory.getLogger(ElevenLabsAPI::class.java)
  private val client: ElevenLabsClient = ElevenLabsClient(apiKey)
  private val textToSpeechService: TextToSpeechService = TextToSpeechService(client)
  private val speechToTextService: SpeechToTextService = SpeechToTextService(client)

  /**
   * Converts text to speech using Eleven Labs API
   *
   * @param text The text to convert to speech
   * @param voiceId The ID of the voice to use (defaults to "21m00Tcm4TlvDq8ikWAM")
   * @return The audio data as a ByteArray
   * @throws Exception if the API call fails
   */
  suspend fun textToSpeech(text: String, voiceId: String = "21m00Tcm4TlvDq8ikWAM"): ByteArray {
    logger.debug("Converting text to speech: $text")
    return textToSpeechService.generateSpeech(text, voiceId)
  }

  /**
   * Converts speech to text using Eleven Labs API
   *
   * Note: This is a stub implementation. The actual implementation will be added in the future.
   *
   * @param audioData The audio data to convert to text
   * @param modelId The ID of the model to use (defaults to "eleven_multilingual_v1")
   * @return The recognized text and confidence score
   * @throws NotImplementedError as this is a stub implementation
   */
  suspend fun speechToText(
      audioData: ByteArray,
      modelId: String = "eleven_multilingual_v1"
  ): SpeechToTextResponse {
    logger.debug("Converting speech to text")
    return speechToTextService.recognizeSpeech(audioData, modelId)
  }

  /**
   * Converts speech from a file to text using Eleven Labs API
   *
   * Note: This is a stub implementation. The actual implementation will be added in the future.
   *
   * @param filePath The path to the audio file to convert to text
   * @param modelId The ID of the model to use (defaults to "eleven_multilingual_v1")
   * @return The recognized text and confidence score
   * @throws NotImplementedError as this is a stub implementation
   */
  suspend fun speechToTextFromFile(
      filePath: String,
      modelId: String = "eleven_multilingual_v1"
  ): SpeechToTextResponse {
    logger.debug("Converting speech from file to text: $filePath")
    return speechToTextService.recognizeSpeechFromFile(filePath, modelId)
  }

  companion object {
    /**
     * Creates an instance of ElevenLabsAPI using the API key from the environment variable
     *
     * @return An instance of ElevenLabsAPI
     * @throws IllegalStateException if the ELEVEN_LABS_API_KEY environment variable is not set
     */
    fun fromEnvironment(): ElevenLabsAPI {
      val apiKey =
          System.getenv("ELEVEN_LABS_API_KEY")
              ?: throw IllegalStateException("ELEVEN_LABS_API_KEY environment variable is not set")
      return ElevenLabsAPI(apiKey)
    }
  }
}
