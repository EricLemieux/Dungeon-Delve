package com.lemieuxdev.llm.examples

import com.lemieuxdev.llm.DefaultLLMDataExtractor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example demonstrating how to use the LLMDataExtractor interface. This example shows how to
 * extract structured data from an LLM response.
 */
object LLMDataExtractorExample {
  /** Example data class that will be populated from the LLM response. */
  @Serializable
  data class Character(
      val name: String,
      val class_: String,
      val level: Int,
      val attributes: Attributes,
      val backstory: String
  )

  /** Nested data class for character attributes. */
  @Serializable
  data class Attributes(
      val strength: Int,
      val dexterity: Int,
      val constitution: Int,
      val intelligence: Int,
      val wisdom: Int,
      val charisma: Int
  )

  /** Example of extracting raw JSON data from an LLM response. */
  fun extractRawJsonExample() {
    runBlocking {
      // Create an instance of DefaultLLMDataExtractor using OpenAI
      val extractor = DefaultLLMDataExtractor.withOpenAI()

      // Define a prompt that asks for data in a format that can be represented as JSON
      val prompt =
          "Create a fantasy character with a name, class, level, attributes, and backstory."

      // Optional system prompt to guide the LLM
      val systemPrompt =
          """
                You are a character creation assistant for a fantasy role-playing game.
                Create detailed and creative characters with rich backstories.
            """
              .trimIndent()

      try {
        // Extract the data as a JsonElement
        val jsonData: JsonElement = extractor.extractData(prompt, systemPrompt)

        // Process the JSON data
        println("Character data: $jsonData")

        // You can also access specific fields
        if (jsonData is JsonObject) {
          val name = jsonData["name"]?.jsonPrimitive?.content
          println("Character name: $name")
        }
      } catch (e: Exception) {
        println("Error extracting data: ${e.message}")
      }
    }
  }

  /** Example of extracting typed data from an LLM response. */
  fun extractTypedDataExample() {
    runBlocking {
      // Create an instance of DefaultLLMDataExtractor using OpenAI
      val extractor = DefaultLLMDataExtractor.withOpenAI()

      // Define a prompt that asks for data in a format that matches our Character class
      val prompt =
          "Create a fantasy character with a name, class, level, attributes (strength, dexterity, constitution, intelligence, wisdom, charisma), and backstory."

      // Optional system prompt to guide the LLM
      val systemPrompt =
          """
                You are a character creation assistant for a fantasy role-playing game.
                Create detailed and creative characters with rich backstories.

                The response should include the following fields:
                - name: string
                - class_: string (the character's class like warrior, mage, etc.)
                - level: integer between 1 and 20
                - attributes: object with the following fields:
                  - strength: integer between 1 and 20
                  - dexterity: integer between 1 and 20
                  - constitution: integer between 1 and 20
                  - intelligence: integer between 1 and 20
                  - wisdom: integer between 1 and 20
                  - charisma: integer between 1 and 20
                - backstory: string (a paragraph describing the character's background)
            """
              .trimIndent()

      try {
        // Extract the data as a Character object
        val character: Character =
            extractor.extractData(
                prompt = prompt, serializer = Character.serializer(), systemPrompt = systemPrompt)

        // Use the typed data
        println("Created character: ${character.name}")
        println("Class: ${character.class_}")
        println("Level: ${character.level}")
        println("Strength: ${character.attributes.strength}")
        println("Backstory: ${character.backstory}")
      } catch (e: Exception) {
        println("Error extracting data: ${e.message}")
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
  }
}
