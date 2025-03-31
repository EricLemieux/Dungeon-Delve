package com.lemieuxdev.elevenlabs.models

import kotlinx.serialization.Serializable

/** Request model for Eleven Labs text-to-speech API */
@Serializable
data class TextToSpeechRequest(
    val text: String,
    val model_id: String = "eleven_monolingual_v1",
    val voice_settings: VoiceSettings = VoiceSettings()
)

/** Voice settings for Eleven Labs text-to-speech API */
@Serializable
data class VoiceSettings(val stability: Float = 0.5f, val similarity_boost: Float = 0.5f)
