package com.lemieuxdev.llm.examples

import com.lemieuxdev.llm.SceneArchetype
import com.lemieuxdev.llm.SceneDescriptionExtractor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example demonstrating how to use the SceneDescriptionExtractor class. This example shows how to
 * extract scene descriptions from an LLM response.
 */
object SceneDescriptionExtractorExample {

  /** Example of extracting raw JSON scene description from an LLM response. */
  fun extractRawJsonExample() {
    runBlocking {
      // Create an instance of SceneDescriptionExtractor using OpenAI
      val extractor = SceneDescriptionExtractor.withOpenAI()

      // Define a prompt that describes a scene to be detailed
      val prompt = "Describe a dark forest at midnight during a full moon."

      // Optional custom system prompt (if not provided, the default will be used)
      val customSystemPrompt =
          """
                You are a horror story narrator specializing in creating eerie and unsettling scene descriptions.
                Focus on creating an atmosphere of dread and suspense.
                Emphasize shadows, strange sounds, and unsettling details that might make the reader feel uneasy.
            """
              .trimIndent()

      try {
        // Extract the data as a JsonElement using the custom system prompt
        val jsonData: JsonElement = extractor.extractData(prompt, customSystemPrompt)

        // Process the JSON data
        println("Scene description: $jsonData")

        // You can also access specific fields
        if (jsonData is JsonObject) {
          val scene = jsonData["scene"]?.jsonObject
          val visual = scene?.get("visual")?.jsonPrimitive?.content
          println("Visual description: $visual")

          val pointsOfInterest = jsonData["points_of_interest"]
          println("Points of interest: $pointsOfInterest")
        }
      } catch (e: Exception) {
        println("Error extracting data: ${e.message}")
      }

      try {
        // Extract the data as a JsonElement using the default system prompt
        val jsonData: JsonElement = extractor.extractData(prompt)

        // Process the JSON data
        println("\nScene description with default system prompt: $jsonData")
      } catch (e: Exception) {
        println("Error extracting data with default system prompt: ${e.message}")
      }
    }
  }

  /** Example of extracting typed scene description data from an LLM response. */
  fun extractTypedDataExample() {
    runBlocking {
      // Create an instance of SceneDescriptionExtractor using OpenAI
      val extractor = SceneDescriptionExtractor.withOpenAI()

      // Define a prompt that describes a scene to be detailed
      val prompt = "Describe a bustling medieval marketplace at midday."

      try {
        // Extract the data as a SceneDescription object using the default system prompt
        val sceneDescription: SceneDescriptionExtractor.SceneDescription =
            extractor.extractData(
                prompt = prompt,
                serializer = SceneDescriptionExtractor.SceneDescription.serializer())

        // Use the typed data
        println("Scene Visual: ${sceneDescription.scene.visual}")
        println("Scene Sounds: ${sceneDescription.scene.sounds}")
        println("Scene Smells: ${sceneDescription.scene.smells}")
        println("Scene Atmosphere: ${sceneDescription.scene.atmosphere}")
        println("Scene Layout: ${sceneDescription.scene.layout}")

        println("\nPoints of Interest:")
        sceneDescription.points_of_interest.forEach { poi ->
          println("- ${poi.name}: ${poi.description}")
        }
      } catch (e: Exception) {
        println("Error extracting typed data: ${e.message}")
      }
    }
  }

  /** Example of using different scene archetypes to generate themed scene descriptions. */
  fun extractWithArchetypesExample() {
    runBlocking {
      // Create an instance of SceneDescriptionExtractor using OpenAI
      val extractor = SceneDescriptionExtractor.withOpenAI()

      // Define a base prompt that we'll use with different archetypes
      val basePrompt = "Describe a castle on a hill."

      // Try different archetypes
      val archetypes =
          listOf(
              SceneArchetype.SWORDS_AND_DRAGONS,
              SceneArchetype.ELDRITCH_HORROR,
              SceneArchetype.STEAMPUNK,
              SceneArchetype.FAIRY_TALE)

      for (archetype in archetypes) {
        try {
          println("\n=== ${archetype.name} ARCHETYPE ===")
          println("Description: ${archetype.description}")

          // Extract the data using the specified archetype
          val sceneDescription: SceneDescriptionExtractor.SceneDescription =
              extractor.extractData(
                  prompt = basePrompt,
                  serializer = SceneDescriptionExtractor.SceneDescription.serializer(),
                  systemPrompt = null,
                  archetype = archetype)

          // Print the visual description to see how the archetype affects it
          println("\nVisual Description:")
          println(sceneDescription.scene.visual)

          // Print one point of interest
          if (sceneDescription.points_of_interest.isNotEmpty()) {
            val poi = sceneDescription.points_of_interest.first()
            println("\nPoint of Interest: ${poi.name}")
            println(poi.description)
          }
        } catch (e: Exception) {
          println("Error extracting data with ${archetype.name} archetype: ${e.message}")
        }
      }
    }
  }

  /** Main function to run the examples. */
  @JvmStatic
  fun main(args: Array<String>) {
    println("Running raw JSON example:")
    extractRawJsonExample()

    println("\nRunning typed data example:")
    extractTypedDataExample()

    println("\nRunning archetypes example:")
    extractWithArchetypesExample()
  }
}
