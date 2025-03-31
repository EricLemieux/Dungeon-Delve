package com.lemieuxdev.llm.services

import com.lemieuxdev.llm.client.OpenAIClient
import com.lemieuxdev.llm.models.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service for interacting with the OpenAI API
 */
class OpenAIService(private val client: OpenAIClient) {
    private val logger: Logger = LoggerFactory.getLogger(OpenAIService::class.java)
    
    /**
     * Completes the given prompt using the OpenAI Chat Completions API
     * 
     * @param request The LLM request containing the prompt and configuration
     * @return The LLM response containing the completed text
     * @throws Exception if the API call fails
     */
    suspend fun complete(request: LLMRequest): LLMResponse {
        logger.debug("Completing prompt: ${request.prompt}")
        
        val openAIRequest = OpenAIChatCompletionRequest(
            model = request.config.model,
            messages = listOf(
                OpenAIChatMessage(
                    role = "user",
                    content = request.prompt
                )
            ),
            temperature = request.config.temperature,
            maxTokens = request.config.maxTokens
        )
        
        logger.debug("Sending request to OpenAI API")
        val response = client.httpClient.post("${OpenAIClient.BASE_URL}/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer ${client.getApiKey()}")
            setBody(openAIRequest)
        }
        
        logger.debug("Received response from OpenAI API with status: ${response.status}")
        
        if (response.status.isSuccess()) {
            val openAIResponse = response.body<OpenAIChatCompletionResponse>()
            logger.debug("Successfully completed prompt")
            
            return LLMResponse(
                text = openAIResponse.choices.firstOrNull()?.message?.content ?: "",
                model = openAIResponse.model,
                promptTokens = openAIResponse.usage?.promptTokens,
                completionTokens = openAIResponse.usage?.completionTokens,
                totalTokens = openAIResponse.usage?.totalTokens
            )
        } else {
            logger.error("Failed to complete prompt: ${response.bodyAsText()}")
            throw Exception("Failed to complete prompt: ${response.status}")
        }
    }
}