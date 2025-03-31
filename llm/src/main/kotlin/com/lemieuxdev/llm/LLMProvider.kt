package com.lemieuxdev.llm

import com.lemieuxdev.llm.models.LLMRequest
import com.lemieuxdev.llm.models.LLMResponse

/** Interface for Large Language Model providers */
interface LLMProvider {
  /**
   * Completes the given prompt using the LLM
   *
   * @param request The request containing the prompt and configuration
   * @return The response containing the completed text
   * @throws Exception if the API call fails
   */
  suspend fun complete(request: LLMRequest): LLMResponse
}
