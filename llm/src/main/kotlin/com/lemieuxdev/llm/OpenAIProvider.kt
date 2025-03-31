package com.lemieuxdev.llm

import com.lemieuxdev.llm.client.OpenAIClient
import com.lemieuxdev.llm.models.LLMRequest
import com.lemieuxdev.llm.models.LLMResponse
import com.lemieuxdev.llm.services.OpenAIService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implementation of LLMProvider for OpenAI
 */
class OpenAIProvider(apiKey: String) : LLMProvider {
    private val logger: Logger = LoggerFactory.getLogger(OpenAIProvider::class.java)
    private val client: OpenAIClient = OpenAIClient(apiKey)
    private val service: OpenAIService = OpenAIService(client)
    
    /**
     * Completes the given prompt using the OpenAI API
     * 
     * @param request The request containing the prompt and configuration
     * @return The response containing the completed text
     * @throws Exception if the API call fails
     */
    override suspend fun complete(request: LLMRequest): LLMResponse {
        logger.debug("Completing prompt with OpenAI: ${request.prompt}")
        return service.complete(request)
    }
    
    companion object {
        /**
         * Creates an instance of OpenAIProvider using the API key from the environment variable
         * 
         * @return An instance of OpenAIProvider
         * @throws IllegalStateException if the OPENAI_API_KEY environment variable is not set
         */
        fun fromEnvironment(): OpenAIProvider {
            val apiKey = System.getenv("OPENAI_API_KEY") 
                ?: throw IllegalStateException("OPENAI_API_KEY environment variable is not set")
            return OpenAIProvider(apiKey)
        }
    }
}