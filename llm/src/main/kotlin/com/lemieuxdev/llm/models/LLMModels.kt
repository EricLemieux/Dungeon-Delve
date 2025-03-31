package com.lemieuxdev.llm.models

import kotlinx.serialization.Serializable

/** Configuration for LLM requests */
@Serializable
data class LLMConfig(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val model: String = "gpt-3.5-turbo"
)

/** Request model for LLM API */
@Serializable
data class LLMRequest(
    val prompt: String,
    val config: LLMConfig = LLMConfig(),
    val character: Character? = null
)

/** Response model for LLM API */
@Serializable
data class LLMResponse(
    val text: String,
    val model: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)
