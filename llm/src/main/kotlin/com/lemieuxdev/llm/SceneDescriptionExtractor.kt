package com.lemieuxdev.llm

import com.lemieuxdev.llm.models.LLMRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implementation of LLMDataExtractor for extracting scene descriptions. This class is used to
 * generate detailed descriptions of scenes that a character can see.
 */
class SceneDescriptionExtractor(
    private val llmProvider: LLMProvider,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : LLMDataExtractor {

  companion object {
    /**
     * Creates an instance of SceneDescriptionExtractor using an OpenAIProvider from the
     * environment.
     *
     * @return An instance of SceneDescriptionExtractor
     * @throws IllegalStateException if the OPENAI_API_KEY environment variable is not set
     */
    fun withOpenAI(): SceneDescriptionExtractor {
      val provider = OpenAIProvider.fromEnvironment()
      return SceneDescriptionExtractor(provider)
    }

    /**
     * The default system prompt for scene description. This prompt guides the LLM to generate
     * detailed and immersive scene descriptions.
     */
    val DEFAULT_SYSTEM_PROMPT =
        """
            You are a descriptive narrator for a fantasy role-playing game.
            Your task is to create vivid, detailed descriptions of scenes that a character can see.

            Focus on the following aspects:
            - Visual elements (colors, shapes, lighting, objects, creatures, people)
            - Sounds and ambient noise
            - Smells and scents
            - Atmosphere and mood
            - Spatial relationships and layout
            - Notable features or points of interest

            Your descriptions should be immersive and help the player visualize the scene clearly.
            Avoid making assumptions about the character's actions or feelings.
            Stick to describing what can be objectively observed in the scene.
        """
            .trimIndent()
  }

  private val logger: Logger = LoggerFactory.getLogger(SceneDescriptionExtractor::class.java)

  /**
   * Extracts a scene description from an LLM response based on the given prompt.
   *
   * @param prompt The text prompt describing the scene to be detailed
   * @param systemPrompt Optional system prompt to guide the LLM in generating the scene
   *   description, defaults to DEFAULT_SYSTEM_PROMPT if not provided
   * @return A JsonElement containing the structured scene description
   * @throws Exception if the extraction fails
   */
  override suspend fun extractData(prompt: String, systemPrompt: String?): JsonElement {
    val effectiveSystemPrompt = systemPrompt ?: DEFAULT_SYSTEM_PROMPT
    val fullPrompt = buildPrompt(prompt, effectiveSystemPrompt)
    logger.debug("Extracting scene description with prompt: $fullPrompt")

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
   * Extracts a scene description from an LLM response and parses it into a specific type.
   *
   * @param prompt The text prompt describing the scene to be detailed
   * @param serializer The serializer to use for parsing the JSON response
   * @param systemPrompt Optional system prompt to guide the LLM in generating the scene
   *   description, defaults to DEFAULT_SYSTEM_PROMPT if not provided
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
   * @param prompt The user's prompt describing the scene
   * @param systemPrompt The system prompt for scene description
   * @return The full prompt with JSON instructions
   */
  private fun buildPrompt(prompt: String, systemPrompt: String): String {
    val jsonInstructions =
        """
            Return your response in valid JSON format with the following structure:
            {
              "scene": {
                "visual": "Description of what can be seen",
                "sounds": "Description of what can be heard",
                "smells": "Description of what can be smelled",
                "atmosphere": "Description of the overall mood and feeling",
                "layout": "Description of the spatial arrangement"
              },
              "points_of_interest": [
                {
                  "name": "Name of the point of interest",
                  "description": "Detailed description of this point of interest"
                }
              ]
            }

            Do not include any explanations, only provide a RFC8259 compliant JSON response.
            Ensure the output can be parsed by a standard JSON parser.
        """
            .trimIndent()

    val fullSystemPrompt =
        """
            $systemPrompt

            $jsonInstructions
        """
            .trimIndent()

    return """
            System: $fullSystemPrompt

            User: $prompt
        """
        .trimIndent()
  }

  /**
   * Data class representing a scene description. This can be used with the typed extractData
   * method.
   */
  @Serializable
  data class SceneDescription(val scene: Scene, val points_of_interest: List<PointOfInterest>)

  /** Data class representing the details of a scene. */
  @Serializable
  data class Scene(
      val visual: String,
      val sounds: String,
      val smells: String,
      val atmosphere: String,
      val layout: String
  )

  /** Data class representing a point of interest in a scene. */
  @Serializable data class PointOfInterest(val name: String, val description: String)
}
