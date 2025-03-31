package com.lemieuxdev.llm

import com.lemieuxdev.llm.models.LLMConfig
import com.lemieuxdev.llm.models.LLMRequest
import com.lemieuxdev.llm.models.LLMResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** Main API class for interacting with Large Language Models */
class LLMAPI(private val provider: LLMProvider) {
  private val logger: Logger = LoggerFactory.getLogger(LLMAPI::class.java)

  /**
   * Completes the given prompt using the LLM
   *
   * @param prompt The prompt to complete
   * @param config The configuration for the LLM request
   * @return The response containing the completed text
   * @throws Exception if the API call fails
   */
  suspend fun complete(prompt: String, config: LLMConfig = LLMConfig()): LLMResponse {
    logger.debug("Completing prompt: $prompt")
    val request = LLMRequest(prompt = prompt, config = config)
    return provider.complete(request)
  }

  companion object {
    /**
     * Creates an instance of LLMAPI using the OpenAI provider with the API key from the environment
     * variable
     *
     * @return An instance of LLMAPI
     * @throws IllegalStateException if the OPENAI_API_KEY environment variable is not set
     */
    fun fromEnvironment(): LLMAPI {
      return LLMAPI(OpenAIProvider.fromEnvironment())
    }
  }
}
