package com.lemieuxdev.llm

import kotlinx.serialization.json.JsonElement

/**
 * Interface for extracting structured data from LLM responses. This interface provides a way to
 * send a prompt to an LLM and get back structured data in JSON format.
 */
interface LLMDataExtractor {
  /**
   * Extracts structured data from an LLM response based on the given prompt.
   *
   * @param prompt The text prompt to send to the LLM
   * @param systemPrompt Optional system prompt to guide the LLM in generating structured data
   * @return A JsonElement containing the structured data extracted from the LLM response
   * @throws Exception if the extraction fails
   */
  suspend fun extractData(prompt: String, systemPrompt: String? = null): JsonElement

  /**
   * Extracts structured data from an LLM response and parses it into a specific type.
   *
   * @param prompt The text prompt to send to the LLM
   * @param systemPrompt Optional system prompt to guide the LLM in generating structured data
   * @param serializer The serializer to use for parsing the JSON response
   * @return An instance of type T parsed from the JSON response
   * @throws Exception if the extraction or parsing fails
   */
  suspend fun <T> extractData(
      prompt: String,
      serializer: kotlinx.serialization.KSerializer<T>,
      systemPrompt: String? = null
  ): T
}
