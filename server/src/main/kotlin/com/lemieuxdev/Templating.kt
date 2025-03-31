package com.lemieuxdev

import com.lemieuxdev.Adventure.AdventureState
import com.lemieuxdev.Scene.SceneState
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.html.*
import kotlinx.html.stream.createHTML

class Action(
    val name: String,

    /**
     * Expression to be run when the action is triggered. There are pre- and post-processing steps
     * that can also be configured to run.
     */
    private val process: suspend () -> Unit = {},
) {
  private val preProcess: suspend () -> Unit = {}
  private val postProcess: suspend () -> Unit = {
    val gameBoard = createHTML().main { gameBoard(game) }
    sseEvents.emit(gameBoard)
  }

  /**
   * This is the way that the action should be called to ensure that the pre and post-processing
   * steps are being called.
   */
  suspend fun run() {
    preProcess()
    process()
    postProcess()
  }
}

/**
 * Highest level, this is the adventure, it would contain the game state, and the scenes for the
 * adventure.
 */
interface Adventure {
  var state: AdventureState

  interface AdventureState {
    var actions: List<Action>
    var friendlyCharacters: MutableList<Character>
  }
}

class DefaultAdventureState() : AdventureState {
  override var actions: List<Action> = listOf(Action("test") { println("Testing") })
  override var friendlyCharacters: MutableList<Character> = mutableListOf(
    Character("Hero", false, 100, 10),
    Character("Companion", false, 75, 7)
  )
}

/** This is an individual scene, adventures are made up of scenes that connect to each other. */
interface Scene {
  var state: SceneState

  interface SceneState {
    var outputText: String
    var actions: List<Action>
    var showCursor: Boolean
  }
}

class DefaultSceneState(
    override var outputText: String = "",
    override var actions: List<Action> = emptyList(),
    override var showCursor: Boolean = false
) : SceneState

/** Combat scene state that stores characters (enemies and friendlies) */
class CombatSceneState(
    override var outputText: String = "",
    override var actions: List<Action> = emptyList(),
    override var showCursor: Boolean = false,
    var characters: MutableList<Character> = mutableListOf(),
    var selectedEnemyIndex: Int? = null,
    var turnOrder: MutableList<Character> = mutableListOf(),
    var currentTurnIndex: Int = 0
) : SceneState {
    // Get all enemy characters
    fun getEnemies(): List<Character> = characters.filter { it.isEnemy }

    // Get all friendly characters
    fun getFriendlies(): List<Character> = characters.filter { !it.isEnemy }

    // Add a character to the combat scene
    fun addCharacter(character: Character) {
        characters.add(character)
    }

    // Remove a character from the combat scene (e.g., when defeated)
    fun removeCharacter(character: Character) {
        characters.remove(character)
        turnOrder.remove(character)

        // Adjust currentTurnIndex if needed
        if (turnOrder.isNotEmpty() && currentTurnIndex >= turnOrder.size) {
            currentTurnIndex = 0
        }
    }

    // Get the selected enemy
    fun getSelectedEnemy(): Character? {
        val enemies = getEnemies()
        return if (selectedEnemyIndex != null && selectedEnemyIndex!! < enemies.size) {
            enemies[selectedEnemyIndex!!]
        } else {
            null
        }
    }

    // Get the character whose turn it currently is
    fun getCurrentTurnCharacter(): Character? {
        return if (turnOrder.isNotEmpty() && currentTurnIndex < turnOrder.size) {
            turnOrder[currentTurnIndex]
        } else {
            null
        }
    }

    // Advance to the next character's turn
    fun advanceToNextTurn() {
        if (turnOrder.isNotEmpty()) {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size
        }
    }
}

/** Combat scene implementation */
class CombatScene(override var state: SceneState = CombatSceneState()) : Scene {
    // Initialize the combat scene with characters
    fun initialize(enemies: List<Character>, friendlies: List<Character>) {
        val combatState = state as CombatSceneState
        combatState.characters.clear()
        combatState.characters.addAll(enemies)
        combatState.characters.addAll(friendlies)

        // Initialize turn order by shuffling all characters
        combatState.turnOrder.clear()
        combatState.turnOrder.addAll(combatState.characters.shuffled())
        combatState.currentTurnIndex = 0

        val currentCharacter = combatState.getCurrentTurnCharacter()

        // Set initial output text
        combatState.outputText = "Combat has begun! ${currentCharacter?.name ?: "Unknown"}'s turn."

        // Set up initial actions
        updateActions()
    }

    // Update available actions based on the current state
    fun updateActions() {
        val combatState = state as CombatSceneState
        val actions = mutableListOf<Action>()

        val currentCharacter = combatState.getCurrentTurnCharacter()

        // Only allow actions if it's a friendly character's turn
        if (currentCharacter != null && !currentCharacter.isEnemy) {
            // Add attack action if an enemy is selected
            val selectedEnemy = combatState.getSelectedEnemy()
            if (selectedEnemy != null) {
                actions.add(Action("Attack ${selectedEnemy.name}") {
                    // Perform attack logic
                    selectedEnemy.health -= currentCharacter.attack
                    combatState.outputText = "${currentCharacter.name} attacks ${selectedEnemy.name} for ${currentCharacter.attack} damage!"

                    // Check if enemy is defeated
                    if (selectedEnemy.health <= 0) {
                        combatState.outputText += "\n${selectedEnemy.name} has been defeated!"
                        combatState.removeCharacter(selectedEnemy)
                    }

                    // Reset selected enemy and advance to next turn
                    combatState.selectedEnemyIndex = null
                    endTurn()
                })
            }

            // Add enemy selection actions
            val enemies = combatState.getEnemies()
            enemies.forEachIndexed { index, enemy ->
                actions.add(Action("Select ${enemy.name}") {
                    combatState.selectedEnemyIndex = index
                    combatState.outputText = "${enemy.name} selected as target."
                    updateActions()
                })
            }
        } else if (currentCharacter != null && currentCharacter.isEnemy) {
            // If it's an enemy's turn, automatically attack a random friendly
            val friendlies = combatState.getFriendlies()
            if (friendlies.isNotEmpty()) {
                val target = friendlies.random()
                target.health -= currentCharacter.attack
                combatState.outputText = "${currentCharacter.name} attacks ${target.name} for ${currentCharacter.attack} damage!"

                // Check if friendly is defeated
                if (target.health <= 0) {
                    combatState.outputText += "\n${target.name} has been defeated!"
                    combatState.removeCharacter(target)
                }

                // End the enemy's turn
                endTurn()
            } else {
                // If no friendlies left, just end the turn
                endTurn()
            }
        }

        combatState.actions = actions
    }

    // End the current turn and advance to the next character
    fun endTurn() {
        val combatState = state as CombatSceneState
        combatState.advanceToNextTurn()

        val nextCharacter = combatState.getCurrentTurnCharacter()
        if (nextCharacter != null) {
            combatState.outputText += "\nIt's now ${nextCharacter.name}'s turn."
        }

        updateActions()
    }
}

/** Display controls, this dictates the presentation of the game state to the users. */
interface Display {
  /** Render as html */
  fun render(adventureState: AdventureState, sceneState: SceneState): FlowContent.() -> Unit {
    println("rendering")
    return { h1 { +"TODO" } }
  }
}

class TerminalDisplay() : Display {
  override fun render(
      adventureState: AdventureState,
      sceneState: SceneState
  ): FlowContent.() -> Unit {
    return {
      div {
        classes = "flex min-h-screen items-center justify-center bg-gray-900 p-4".split(" ").toSet()

        div {
          classes =
              "relative w-full max-w-3xl overflow-hidden rounded-lg border-8 border-gray-800 bg-gray-900 shadow-2xl"
                  .split(" ")
                  .toSet()

          // Monitor frame gradient
          div {
            classes =
                "absolute inset-0 rounded-sm bg-gradient-to-b from-gray-800 to-gray-700 opacity-10"
                    .split(" ")
                    .toSet()
          }

          // Power indicator
          div {
            classes = "absolute right-4 top-4 flex items-center gap-2".split(" ").toSet()

            div {
              classes =
                  "h-2 w-2 rounded-full bg-green-500 shadow-[0_0_8px_2px_rgba(74,222,128,0.6)]"
                      .split(" ")
                      .toSet()
            }

            // Terminal icon placeholder (you might need to add an actual icon here)
            div { classes = "h-4 w-4 text-gray-600".split(" ").toSet() }
          }

          // Screen with CRT effect
          div {
            classes =
                "relative overflow-hidden rounded-sm bg-black p-6 shadow-inner".split(" ").toSet()

            // CRT glow
            div {
              classes =
                  "pointer-events-none absolute inset-0 rounded-sm bg-green-500 opacity-[0.03] mix-blend-screen"
                      .split(" ")
                      .toSet()
            }

            // Scan lines
            div {
              classes =
                  "pointer-events-none absolute inset-0 bg-[linear-gradient(to_bottom,transparent,transparent_2px,rgba(0,0,0,0.05)_3px,transparent_3px)] bg-[length:100%_4px]"
                      .split(" ")
                      .toSet()
            }

            // Screen curvature
            div {
              classes =
                  "pointer-events-none absolute inset-0 rounded-sm bg-[radial-gradient(circle_at_center,transparent_30%,rgba(0,0,0,0.3)_100%)]"
                      .split(" ")
                      .toSet()
            }

            // Terminal content
            div {
              classes =
                  "flex flex-col gap-2 relative min-h-[60vh] font-mono text-sm text-green-500 md:text-base"
                      .split(" ")
                      .toSet()

              // Output text
              pre {
                classes = "whitespace-pre-wrap".split(" ").toSet()
                +sceneState.outputText
              }
              // Blinking cursor
              if (sceneState.showCursor) {
                span {
                  classes = "inline-block h-4 w-2 animate-blink bg-green-500".split(" ").toSet()
                  +" "
                }
              }

              // Game controls
              // Render buttons from scene state actions
              for (action in sceneState.actions) {
                button {
                  attributes["hx-post"] = "/action/${action.name}"
                  attributes["hx-swap"] = "none"
                  classes =
                      "px-6 py-2 bg-green-600 hover:bg-green-700 text-black font-mono rounded border-2 border-green-500 transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-opacity-50 active:bg-green-800 mb-4"
                          .split(" ")
                          .toSet()
                  +action.name
                }
              }
            }
          }

          // Monitor base
          div { classes = "h-4 rounded-b-lg bg-gray-800".split(" ").toSet() }
        }
      }
    }
  }
}

// Character data class that can represent both enemies and friendlies
data class Character(
    val name: String,
    val isEnemy: Boolean,
    var health: Int,
    var attack: Int
)

// TODO: Refactor or remove this
class Game(
    var player: Player?,
    var monster: Monster?,
)

class Player(var attack: Int = 10)

fun Player.hitMonster(monster: Monster) {
  monster.health -= attack
}

class Monster(var health: Int = 50)

val game = Game(player = Player(), monster = Monster())

val sseEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)

fun HTML.gameBoardWrapper(gameState: Game) {
  head {
    title { +"Dungeon Delve" }
    // PWA meta tags
    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
    meta(name = "theme-color", content = "#000000")
    meta(name = "description", content = "A text-based dungeon adventure game")

    // PWA manifest
    link(rel = "manifest", href = "/manifest.json")

    // PWA icons
    link(rel = "icon", href = "/icons/icon-192x192.html")
    link(rel = "apple-touch-icon", href = "/icons/icon-192x192.html")

    // Stylesheets and scripts
    link(rel = "stylesheet", href = "/static/output.css")
    script(src = "https://unpkg.com/htmx.org@2.0.4") {}
    script(src = "https://unpkg.com/htmx-ext-sse@2.2.2") {}

    script {
      unsafe {
        // language=javascript
        raw(
            """
                // Theme selection
                if (localStorage.theme === 'light') {
                  document.documentElement.classList.remove('dark')
                } else {
                  document.documentElement.classList.add('dark')
                }

                // Service worker registration
                if ('serviceWorker' in navigator) {
                  window.addEventListener('load', function() {
                    navigator.serviceWorker.register('/sw.js').then(function(registration) {
                      console.log('ServiceWorker registration successful with scope: ', registration.scope);
                    }, function(err) {
                      console.log('ServiceWorker registration failed: ', err);
                    });
                  });
                }
            """
                .trimIndent())
      }
    }
  }
  body {
    attributes["hx-boost"] = "true"
    attributes["hx-ext"] = "sse"
    attributes["sse-connect"] = "/events"
    attributes["sse-swap"] = "message"
    attributes["hx-target"] = "#game-board"

    main { gameBoard(game) }
  }
}

val adventureState: AdventureState = DefaultAdventureState()
val sceneState: SceneState =
    DefaultSceneState(
        actions =
            listOf(
                Action("START") {
                  // Initialize the game
                  game.player?.hitMonster(game.monster!!)

                  // Update scene state
                  sceneState.actions = emptyList()
                  sceneState.showCursor = true

                  // Show text gradually
                  val hardcodedText =
                      "You awaken on a cold, uneven stone floor, the air thick with the damp scent of moss and earth. Faint echoes drip from unseen crevices, and the dim glow of phosphorescent fungi outlines jagged walls around you. As you rise, the weight of silence presses against your ears, broken only by the crunch of gravel beneath your boots. A narrow passage leads you forward, its walls narrowing before spilling you into blinding sunlight. Shielding your eyes, you step into the open and behold a vast, windswept desert stretching endlessly before you. Dominating the horizon stands a colossal pyramid, its golden surface shimmering under the sun, ancient and foreboding. The air hums with a strange energy, and a faint, unearthly whisper brushes your mind, urging you closer."
                  //                          .split(" ")

                  //                  repeat(hardcodedText.size) { i ->
                  //                    delay(0.05.seconds)
                  //                    sceneState.outputText =
                  // hardcodedText.slice(0..i).joinToString(" ")
                  //                    val gameBoard = createHTML().main { gameBoard(game) }
                  //                    sseEvents.emit(gameBoard)
                  //                  }
                  sceneState.outputText = hardcodedText

                  // After the text is fully revealed, add the Approach action
                  sceneState.actions =
                      listOf(
                          Action("Approach") {
                            sceneState.actions = emptyList()
                            sceneState.outputText = "\"Halt!\""

                            // Add action to enter combat
                            sceneState.actions = listOf(
                                Action("Enter Combat") {
                                    // Switch to combat scene
                                    currentScene = combatScene
                                    currentDisplay = combatDisplay

                                    // Initialize combat with enemies and friendlies from adventure state
                                    val enemies = listOf(
                                        Character("Goblin", true, 30, 5),
                                        Character("Orc", true, 50, 8)
                                    )
                                    // Use friendly characters from adventure state
                                    combatScene.initialize(enemies, adventureState.friendlyCharacters)
                                }
                            )
                          })

                  // Keep the cursor visible
                  sceneState.showCursor = true
                }))

// Create combat scene and display
val combatScene = CombatScene()
val combatDisplay = CombatDisplay()

// Track current scene and display
var currentScene: Scene = object : Scene { override var state: SceneState = sceneState }
var currentDisplay: Display = TerminalDisplay()

fun MAIN.gameBoard(gameState: Game) {
  id = "game-board"

  apply(currentDisplay.render(adventureState, currentScene.state))
}

fun Application.configureTemplating() {
  install(SSE)

  routing {
    staticResources("/static", "static")
    staticResources("/", "/web")

    get("/") { call.respondHtml { gameBoardWrapper(game) } }

    post("/action/{actionId}") {
      val actionId = call.parameters["actionId"]
      println("Action ID: $actionId")

      // Find the matching action in the current scene state
      val action = currentScene.state.actions.find { it.name == actionId }

      if (action != null) {
        // Execute the action
        action.run()

        call.respondHtml(HttpStatusCode.OK) {}
      } else {
        call.respondHtml(HttpStatusCode.NotFound) { body { h1 { +"Action not found" } } }
      }
    }

    sse("/events") { sseEvents.collect { event -> send(ServerSentEvent(event)) } }
  }
}

/**
 * The main appliction here should be registering adventures, should be an interface, should take in
 * the event emiter
 */
