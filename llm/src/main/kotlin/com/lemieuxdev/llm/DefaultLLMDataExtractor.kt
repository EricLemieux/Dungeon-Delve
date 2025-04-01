package com.lemieuxdev.llm

import com.lemieuxdev.llm.models.LLMRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Default implementation of LLMDataExtractor that uses an LLMProvider to extract structured data.
 * This implementation prompts the LLM to return data in JSON format and then parses the response.
 */
class DefaultLLMDataExtractor(
    private val llmProvider: LLMProvider,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : LLMDataExtractor {

  companion object {
    /**
     * Creates an instance of DefaultLLMDataExtractor using an OpenAIProvider from the environment.
     *
     * @return An instance of DefaultLLMDataExtractor
     * @throws IllegalStateException if the OPENAI_API_KEY environment variable is not set
     */
    fun withOpenAI(): DefaultLLMDataExtractor {
      val provider = OpenAIProvider.fromEnvironment()
      return DefaultLLMDataExtractor(provider)
    }
  }

  private val logger: Logger = LoggerFactory.getLogger(DefaultLLMDataExtractor::class.java)

  /**
   * Extracts structured data from an LLM response based on the given prompt.
   *
   * @param prompt The text prompt to send to the LLM
   * @param systemPrompt Optional system prompt to guide the LLM in generating structured data
   * @return A JsonElement containing the structured data extracted from the LLM response
   * @throws Exception if the extraction fails
   */
  override suspend fun extractData(prompt: String, systemPrompt: String?): JsonElement {
    val fullPrompt = buildPrompt(prompt, systemPrompt)
    logger.debug("Extracting data with prompt: $fullPrompt")

    val response = llmProvider.complete(LLMRequest(prompt = fullPrompt))
    logger.debug("Received response: ${response.text}")

    return try {
      json.parseToJsonElement(response.text)
    } catch (e: Exception) {
      logger.error("Failed to parse response as JSON: ${response.text}", e)
      throw IllegalStateException("Failed to parse LLM response as JSON", e)
    }
  }

  /**
   * Extracts structured data from an LLM response and parses it into a specific type.
   *
   * @param prompt The text prompt to send to the LLM
   * @param serializer The serializer to use for parsing the JSON response
   * @param systemPrompt Optional system prompt to guide the LLM in generating structured data
   * @return An instance of type T parsed from the JSON response
   * @throws Exception if the extraction or parsing fails
   */
  override suspend fun <T> extractData(
      prompt: String,
      serializer: KSerializer<T>,
      systemPrompt: String?
  ): T {
    val jsonElement = extractData(prompt, systemPrompt)
    return try {
      json.decodeFromJsonElement(serializer, jsonElement)
    } catch (e: Exception) {
      logger.error("Failed to decode JSON to type: $jsonElement", e)
      throw IllegalStateException("Failed to decode JSON response to the requested type", e)
    }
  }

  /**
   * Builds the full prompt to send to the LLM, including instructions to return JSON.
   *
   * @param prompt The user's prompt
   * @param systemPrompt Optional system prompt
   * @return The full prompt with JSON instructions
   */
  private fun buildPrompt(prompt: String, systemPrompt: String?): String {
    val jsonInstructions =
        """
            Return your response in valid JSON format.
            Do not include any explanations, only provide a RFC8259 compliant JSON response.
            Ensure the output can be parsed by a standard JSON parser.
        """
            .trimIndent()

    val fullSystemPrompt =
        if (systemPrompt != null) {
          """
            $systemPrompt

            $jsonInstructions
            """
              .trimIndent()
        } else {
          jsonInstructions
        }

    return """
            System: $fullSystemPrompt

            User: $prompt
        """
        .trimIndent()
  }
}
