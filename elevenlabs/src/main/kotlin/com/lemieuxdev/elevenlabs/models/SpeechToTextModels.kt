package com.lemieuxdev.elevenlabs.models

import kotlinx.serialization.Serializable

/** Request model for Eleven Labs speech-to-text API */
@Serializable
data class SpeechToTextRequest(
    val audioData: ByteArray,
    val model_id: String = "eleven_multilingual_v1"
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SpeechToTextRequest

    if (!audioData.contentEquals(other.audioData)) return false
    if (model_id != other.model_id) return false

    return true
  }

  override fun hashCode(): Int {
    var result = audioData.contentHashCode()
    result = 31 * result + model_id.hashCode()
    return result
  }
}

/** Response model for Eleven Labs speech-to-text API */
@Serializable data class SpeechToTextResponse(val text: String, val confidence: Float = 0.0f)
