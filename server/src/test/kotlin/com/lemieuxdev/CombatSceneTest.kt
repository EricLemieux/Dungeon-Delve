package com.lemieuxdev

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CombatSceneTest {

  @Test
  fun testCombatSceneInitialization() {
    // Create a combat scene
    val combatScene = CombatScene()
    val combatState = CombatSceneState()
    combatScene.state = combatState

    // Create some test characters
    val enemies = listOf(Character("Enemy 1", true, 50, 5), Character("Enemy 2", true, 40, 4))
    val friendlies = listOf(Character("Hero 1", false, 100, 10), Character("Hero 2", false, 80, 8))

    // Initialize the combat scene
    combatScene.initialize(enemies, friendlies)

    // Verify that all characters are added to the state
    assertEquals(4, combatState.characters.size)
    assertEquals(2, combatState.getEnemies().size)
    assertEquals(2, combatState.getFriendlies().size)

    // Verify that turn order is initialized
    assertEquals(4, combatState.turnOrder.size)

    // Verify that the current turn index is set
    assertEquals(0, combatState.currentTurnIndex)

    // Verify that the current turn character is set
    assertNotNull(combatState.getCurrentTurnCharacter())
  }

  @Test
  fun testTurnAdvancement() {
    // Create a combat scene
    val combatScene = CombatScene()
    val combatState = CombatSceneState()
    combatScene.state = combatState

    // Create some test characters
    val enemies = listOf(Character("Enemy 1", true, 50, 5))
    val friendlies = listOf(Character("Hero 1", false, 100, 10))

    // Initialize the combat scene
    combatScene.initialize(enemies, friendlies)

    // Get the initial turn character
    val initialCharacter = combatState.getCurrentTurnCharacter()
    assertNotNull(initialCharacter)

    // End the turn
    runBlocking { combatScene.endTurn() }

    // Verify that the turn has advanced
    val nextCharacter = combatState.getCurrentTurnCharacter()
    assertNotNull(nextCharacter)
    assertTrue(initialCharacter != nextCharacter)

    // End the turn again
    runBlocking { combatScene.endTurn() }

    // Verify that we've cycled back to the first character
    val thirdCharacter = combatState.getCurrentTurnCharacter()
    assertEquals(initialCharacter, thirdCharacter)
  }
}
