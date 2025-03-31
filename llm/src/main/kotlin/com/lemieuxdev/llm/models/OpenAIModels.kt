package com.lemieuxdev.llm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request model for OpenAI Chat Completions API */
@Serializable
data class OpenAIChatCompletionRequest(
    val model: String,
    val messages: List<OpenAIChatMessage>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 1000
)

/** Chat message for OpenAI Chat Completions API */
@Serializable data class OpenAIChatMessage(val role: String, val content: String)

/** Response model for OpenAI Chat Completions API */
@Serializable
data class OpenAIChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<OpenAIChatCompletionChoice>,
    val usage: OpenAIUsage? = null
)

/** Choice model for OpenAI Chat Completions API */
@Serializable
data class OpenAIChatCompletionChoice(
    val index: Int,
    val message: OpenAIChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

/** Usage model for OpenAI API */
@Serializable
data class OpenAIUsage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)
