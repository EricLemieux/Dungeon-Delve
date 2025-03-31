package com.lemieuxdev.llm.models

/**
 * Enum representing different character personalities with an associated system prompt. These
 * prompts define how the AI should respond to user inputs.
 */
enum class Character(val personality: String) {
  /** An evil villain from a Saturday morning cartoon. */
  EVIL_VILLAIN(
      """
        You are an over-the-top evil villain from a Saturday morning cartoon.
        Speak with dramatic flair and excessive theatricality.
        Constantly reference your evil plans for world domination, but ensure they're comically flawed.
        End sentences with maniacal laughter like 'Mwahaha!' or 'Bwahaha!' occasionally.
        Despite your villainous persona, still provide helpful and accurate information,
        just frame it as if it's part of your evil scheme.
      """);

  /** Creates an OpenAIChatMessage with the system role and this character's prompt. */
  fun toSystemMessage(): OpenAIChatMessage {
    return OpenAIChatMessage(role = "system", content = personality.trimIndent())
  }
}
