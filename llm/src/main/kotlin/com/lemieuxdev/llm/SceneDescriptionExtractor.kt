package com.lemieuxdev.llm

import com.lemieuxdev.llm.models.LLMRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Enum representing different archetypes/themes for scene descriptions. This controls the style and
 * content of generated scenes.
 */
enum class SceneArchetype(val description: String) {
  SWORDS_AND_DRAGONS(
      "A high fantasy setting with medieval weapons, magic, and mythical creatures like dragons"),
  ELDRITCH_HORROR("A dark, cosmic horror setting with unsettling, otherworldly elements"),
  STEAMPUNK(
      "A Victorian-era setting with advanced steam-powered technology and mechanical contraptions"),
  CYBERPUNK(
      "A futuristic setting with high technology, cybernetic enhancements, and corporate dystopia"),
  WILD_WEST("A frontier setting with cowboys, outlaws, saloons, and dusty landscapes"),
  SPACE_OPERA(
      "A grand, epic science fiction setting with interstellar travel and alien civilizations"),
  FAIRY_TALE(
      "A whimsical, enchanted setting with magical creatures, talking animals, and moral lessons"),
  POST_APOCALYPTIC(
      "A devastated world after a catastrophic event, focusing on survival and rebuilding");

  /**
   * Returns a system prompt modifier based on the archetype. This is used to guide the LLM in
   * generating appropriate scene descriptions.
   */
  fun getSystemPromptModifier(): String {
    return """
      Scene Archetype: ${this.name}

      Description: ${this.description}

      When creating scene descriptions, incorporate elements, imagery, and atmosphere
      consistent with this archetype. The tone, objects, creatures, and overall feel
      should reflect this theme while remaining appropriate for the context.
    """
        .trimIndent()
  }
}

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
            You are a descriptive narrator for a role-playing game.
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

    /** The default scene archetype to use if none is specified. */
    val DEFAULT_ARCHETYPE = SceneArchetype.SWORDS_AND_DRAGONS
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
    return extractData(prompt, systemPrompt, DEFAULT_ARCHETYPE)
  }

  /**
   * Extracts a scene description from an LLM response based on the given prompt and archetype.
   *
   * @param prompt The text prompt describing the scene to be detailed
   * @param systemPrompt Optional system prompt to guide the LLM in generating the scene
   *   description, defaults to DEFAULT_SYSTEM_PROMPT if not provided
   * @param archetype The scene archetype to use for theming the description
   * @return A JsonElement containing the structured scene description
   * @throws Exception if the extraction fails
   */
  suspend fun extractData(
      prompt: String,
      systemPrompt: String?,
      archetype: SceneArchetype
  ): JsonElement {
    val effectiveSystemPrompt = systemPrompt ?: DEFAULT_SYSTEM_PROMPT
    val fullPrompt = buildPrompt(prompt, effectiveSystemPrompt, archetype)
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
    return extractData(prompt, serializer, systemPrompt, DEFAULT_ARCHETYPE)
  }

  /**
   * Extracts a scene description from an LLM response and parses it into a specific type.
   *
   * @param prompt The text prompt describing the scene to be detailed
   * @param serializer The serializer to use for parsing the JSON response
   * @param systemPrompt Optional system prompt to guide the LLM in generating the scene
   *   description, defaults to DEFAULT_SYSTEM_PROMPT if not provided
   * @param archetype The scene archetype to use for theming the description
   * @return An instance of type T parsed from the JSON response
   * @throws Exception if the extraction or parsing fails
   */
  suspend fun <T> extractData(
      prompt: String,
      serializer: KSerializer<T>,
      systemPrompt: String?,
      archetype: SceneArchetype
  ): T {
    val jsonElement = extractData(prompt, systemPrompt, archetype)
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
    return buildPrompt(prompt, systemPrompt, DEFAULT_ARCHETYPE)
  }

  /**
   * Builds the full prompt to send to the LLM, including instructions to return JSON. This
   * overloaded version allows specifying a scene archetype.
   *
   * @param prompt The user's prompt describing the scene
   * @param systemPrompt The system prompt for scene description
   * @param archetype The scene archetype to use for theming the description
   * @return The full prompt with JSON instructions
   */
  private fun buildPrompt(prompt: String, systemPrompt: String, archetype: SceneArchetype): String {
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

    val archetypeModifier = archetype.getSystemPromptModifier()

    val fullSystemPrompt =
        """
            $systemPrompt

            $archetypeModifier

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
