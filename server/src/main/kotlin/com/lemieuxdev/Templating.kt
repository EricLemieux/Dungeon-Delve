package com.lemieuxdev

import com.lemieuxdev.Adventure.AdventureState
import com.lemieuxdev.Scene.SceneState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class Action(
    val name: String,

    /**
     * Expression to be run when the action is triggered. There are pre- and post-processing steps
     * that can also be configured to run.
     */
    private val process: suspend () -> Unit = {},
) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Action::class.java)
  }
  private val preProcess: suspend () -> Unit = {
    logger.debug("Executing pre-process for action: $name")
  }
  private val postProcess: suspend () -> Unit = {
    logger.debug("Executing post-process for action: $name")
    val gameBoard = createHTML().main { gameBoard(game) }
    logger.debug("Created game board HTML")
    sseEvents.emit(gameBoard)
    logger.debug("Emitted game board to SSE events")
  }

  /**
   * This is the way that the action should be called to ensure that the pre and post-processing
   * steps are being called.
   */
  suspend fun run() {
    logger.debug("Running action: $name")
    logger.debug("Executing pre-process step")
    preProcess()
    logger.debug("Executing main process step")
    process()
    logger.debug("Executing post-process step")
    postProcess()
    logger.debug("Action completed: $name")
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
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(DefaultAdventureState::class.java)
  }

  override var actions: List<Action> = listOf(Action("test") { 
    logger.debug("Executing test action")
    println("Testing") 
  })
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
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CombatSceneState::class.java)
    }
    // Get all enemy characters
    fun getEnemies(): List<Character> {
        logger.debug("Getting all enemy characters")
        return characters.filter { it.isEnemy }
    }

    // Get all friendly characters
    fun getFriendlies(): List<Character> {
        logger.debug("Getting all friendly characters")
        return characters.filter { !it.isEnemy }
    }

    // Add a character to the combat scene
    fun addCharacter(character: Character) {
        logger.debug("Adding character: ${character.name}")
        characters.add(character)
    }

    // Remove a character from the combat scene (e.g., when defeated)
    fun removeCharacter(character: Character) {
        logger.debug("Removing character: ${character.name}")
        characters.remove(character)
        turnOrder.remove(character)

        // Adjust currentTurnIndex if needed
        if (turnOrder.isNotEmpty() && currentTurnIndex >= turnOrder.size) {
            logger.debug("Adjusting currentTurnIndex from $currentTurnIndex to 0 because it's >= turnOrder.size (${turnOrder.size})")
            currentTurnIndex = 0
        } else {
            logger.debug("No need to adjust currentTurnIndex: $currentTurnIndex, turnOrder.size: ${turnOrder.size}")
        }
    }

    // Get the selected enemy
    fun getSelectedEnemy(): Character? {
        logger.debug("Getting selected enemy with index: $selectedEnemyIndex")
        val enemies = getEnemies()
        if (selectedEnemyIndex != null && selectedEnemyIndex!! < enemies.size) {
            logger.debug("Selected enemy found: ${enemies[selectedEnemyIndex!!].name}")
            return enemies[selectedEnemyIndex!!]
        } else {
            logger.debug("No selected enemy found")
            return null
        }
    }

    // Get the character whose turn it currently is
    fun getCurrentTurnCharacter(): Character? {
        logger.debug("Getting current turn character with index: $currentTurnIndex")
        if (turnOrder.isNotEmpty() && currentTurnIndex < turnOrder.size) {
            logger.debug("Current turn character: ${turnOrder[currentTurnIndex].name}")
            return turnOrder[currentTurnIndex]
        } else {
            logger.debug("No current turn character found")
            return null
        }
    }

    // Advance to the next character's turn
    fun advanceToNextTurn() {
        logger.debug("Advancing to next turn from index: $currentTurnIndex")
        if (turnOrder.isNotEmpty()) {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size
            logger.debug("Advanced to next turn, new index: $currentTurnIndex")
        } else {
            logger.debug("Cannot advance turn, turnOrder is empty")
        }
    }
}

/** Combat scene implementation */
class CombatScene(override var state: SceneState = CombatSceneState()) : Scene, CoroutineScope by CoroutineScope(kotlinx.coroutines.Dispatchers.Default) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(CombatScene::class.java)
    }
    // Initialize the combat scene with characters
    fun initialize(enemies: List<Character>, friendlies: List<Character>) {
        logger.debug("Initializing combat scene with ${enemies.size} enemies and ${friendlies.size} friendlies")
        val combatState = state as CombatSceneState
        combatState.characters.clear()
        logger.debug("Adding enemies to combat scene")
        combatState.characters.addAll(enemies)
        logger.debug("Adding friendlies to combat scene")
        combatState.characters.addAll(friendlies)

        // Initialize turn order by shuffling all characters
        logger.debug("Initializing turn order")
        combatState.turnOrder.clear()
        combatState.turnOrder.addAll(combatState.characters.shuffled())
        combatState.currentTurnIndex = 0
        logger.debug("Turn order initialized with ${combatState.turnOrder.size} characters")

        val currentCharacter = combatState.getCurrentTurnCharacter()
        logger.debug("Current character: ${currentCharacter?.name ?: "Unknown"}")

        // Set initial output text
        combatState.outputText = "Combat has begun! ${currentCharacter?.name ?: "Unknown"}'s turn."
        logger.debug("Initial output text set")

        // Set up initial actions
        logger.debug("Setting up initial actions")
        updateActions()

        // Emit the game board state after initialization
        logger.debug("Emitting game board state after initialization")
        launch {
            val gameBoard = createHTML().main { gameBoard(game) }
            sseEvents.emit(gameBoard)
        }
    }

    // Update available actions based on the current state
    fun updateActions() {
        logger.debug("Updating actions")
        val combatState = state as CombatSceneState
        val actions = mutableListOf<Action>()

        val currentCharacter = combatState.getCurrentTurnCharacter()
        logger.debug("Current character: ${currentCharacter?.name ?: "null"}")

        // Only allow actions if it's a friendly character's turn
        if (currentCharacter != null && !currentCharacter.isEnemy) {
            logger.debug("Current character is friendly, adding player actions")

            // Add attack action if an enemy is selected
            val selectedEnemy = combatState.getSelectedEnemy()
            if (selectedEnemy != null) {
                logger.debug("Enemy selected: ${selectedEnemy.name}, adding attack action")
                actions.add(Action("Attack ${selectedEnemy.name}") {
                    logger.debug("Executing attack on ${selectedEnemy.name}")
                    // Perform attack logic
                    selectedEnemy.health -= currentCharacter.attack
                    logger.debug("${currentCharacter.name} attacks ${selectedEnemy.name} for ${currentCharacter.attack} damage, enemy health now: ${selectedEnemy.health}")
                    combatState.outputText = "${currentCharacter.name} attacks ${selectedEnemy.name} for ${currentCharacter.attack} damage!"

                    // Check if enemy is defeated
                    if (selectedEnemy.health <= 0) {
                        logger.debug("${selectedEnemy.name} has been defeated")
                        combatState.outputText += "\n${selectedEnemy.name} has been defeated!"
                        combatState.removeCharacter(selectedEnemy)
                    } else {
                        logger.debug("${selectedEnemy.name} survived with ${selectedEnemy.health} health")
                    }

                    // Reset selected enemy and advance to next turn
                    logger.debug("Resetting selected enemy and ending turn")
                    combatState.selectedEnemyIndex = null
                    endTurn()
                })
            } else {
                logger.debug("No enemy selected, adding enemy selection actions")
            }

            // Add enemy selection actions
            val enemies = combatState.getEnemies()
            logger.debug("Adding selection actions for ${enemies.size} enemies")
            enemies.forEachIndexed { index, enemy ->
                actions.add(Action("Select ${enemy.name}") {
                    logger.debug("Selected enemy: ${enemy.name} at index $index")
                    combatState.selectedEnemyIndex = index
                    combatState.outputText = "${enemy.name} selected as target."
                    updateActions()

                    // Emit the game board state after selecting an enemy
                    logger.debug("Emitting game board state after selecting enemy")
                    val gameBoard = createHTML().main { gameBoard(game) }
                    sseEvents.emit(gameBoard)
                })
            }
        } else if (currentCharacter != null && currentCharacter.isEnemy) {
            logger.debug("Current character is enemy: ${currentCharacter.name}, processing enemy turn")
            // If it's an enemy's turn, show thinking indicator and add delay
            launch {
                processEnemyTurn(currentCharacter)
            }
        } else {
            logger.debug("No current character, no actions added")
        }

        logger.debug("Setting ${actions.size} actions to combat state")
        combatState.actions = actions
    }

    // End the current turn and advance to the next character
    suspend fun endTurn() {
        logger.debug("Ending current turn")
        val combatState = state as CombatSceneState
        combatState.advanceToNextTurn()

        val nextCharacter = combatState.getCurrentTurnCharacter()
        if (nextCharacter != null) {
            logger.debug("Next character's turn: ${nextCharacter.name}")
            combatState.outputText += "\nIt's now ${nextCharacter.name}'s turn."
        } else {
            logger.debug("No next character found")
        }

        logger.debug("Updating actions for next turn")
        updateActions()

        val gameBoard = createHTML().main { gameBoard(game) }
        logger.debug("Created game board HTML")
        sseEvents.emit(gameBoard)
    }

    // Process enemy turn with delay and thinking indicator
    private suspend fun processEnemyTurn(enemy: Character) {
        logger.debug("Processing enemy turn for ${enemy.name}")
        val combatState = state as CombatSceneState
        val friendlies = combatState.getFriendlies()
        logger.debug("Found ${friendlies.size} friendly characters")

        if (friendlies.isNotEmpty()) {
            logger.debug("Friendlies present, enemy will attack")
            // Show thinking indicator
            combatState.showCursor = true
            combatState.outputText = "${enemy.name} is thinking..."
            logger.debug("Set thinking indicator and output text")

            // Emit the game board state to show the thinking indicator
            logger.debug("Emitting game board state to show thinking indicator")
            val gameBoard = createHTML().main { gameBoard(game) }
            sseEvents.emit(gameBoard)

            // TODO: this appears to not be working
            logger.debug("Adding delay to simulate thinking")

            // Add delay to simulate thinking
            delay(2000) // 2 seconds delay
            logger.debug("Delay completed")

            // Select target and attack
            val target = friendlies.random()
            logger.debug("Selected random target: ${target.name}")
            combatState.showCursor = false
            target.health -= enemy.attack
            logger.debug("${enemy.name} attacks ${target.name} for ${enemy.attack} damage, target health now: ${target.health}")
            combatState.outputText = "${enemy.name} attacks ${target.name} for ${enemy.attack} damage!"

            // Check if friendly is defeated
            if (target.health <= 0) {
                logger.debug("${target.name} has been defeated")
                combatState.outputText += "\n${target.name} has been defeated!"
                combatState.removeCharacter(target)
            } else {
                logger.debug("${target.name} survived with ${target.health} health")
            }

            // End the enemy's turn
            logger.debug("Ending enemy's turn")
            endTurn()
        } else {
            // If no friendlies left, just end the turn
            logger.debug("No friendlies left, ending turn without action")
            endTurn()
        }
    }
}

/** Display controls, this dictates the presentation of the game state to the users. */
interface Display {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Display::class.java)
  }

  /** Render as html */
  fun render(adventureState: AdventureState, sceneState: SceneState): FlowContent.() -> Unit {
    logger.debug("Default rendering method called")
    println("rendering")
    return { h1 { +"TODO" } }
  }
}

class TerminalDisplay() : Display {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(TerminalDisplay::class.java)
  }

  override fun render(
      adventureState: AdventureState,
      sceneState: SceneState
  ): FlowContent.() -> Unit {
    logger.debug("Rendering terminal display")
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
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Character::class.java)
    }
}

// TODO: Refactor or remove this
class Game(
    var player: Player?,
    var monster: Monster?,
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(Game::class.java)
    }
}

class Player(var attack: Int = 10) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Player::class.java)
  }
}

fun Player.hitMonster(monster: Monster) {
  val logger: Logger = LoggerFactory.getLogger("com.lemieuxdev.Player.hitMonster")
  logger.debug("Player attacking monster with attack: $attack")
  monster.health -= attack
  logger.debug("Monster health reduced to: ${monster.health}")
}

class Monster(var health: Int = 50) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(Monster::class.java)
  }
}

val game = Game(player = Player(), monster = Monster())

val sseEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)

fun HTML.gameBoardWrapper(gameState: Game) {
  val logger: Logger = LoggerFactory.getLogger("com.lemieuxdev.gameBoardWrapper")
  logger.debug("Rendering game board wrapper")
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
                console.debug('Checking theme preference');
                if (localStorage.theme === 'light') {
                  console.debug('Using light theme');
                  document.documentElement.classList.remove('dark')
                } else {
                  console.debug('Using dark theme');
                  document.documentElement.classList.add('dark')
                }

                // Service worker registration
                console.debug('Checking if service worker is supported');
                if ('serviceWorker' in navigator) {
                  console.debug('Service worker is supported, adding load event listener');
                  window.addEventListener('load', function() {
                    console.debug('Window loaded, registering service worker');
                    navigator.serviceWorker.register('/sw.js').then(function(registration) {
                      console.debug('ServiceWorker registration successful with scope: ', registration.scope);
                      console.log('ServiceWorker registration successful with scope: ', registration.scope);
                    }, function(err) {
                      console.debug('ServiceWorker registration failed: ', err);
                      console.log('ServiceWorker registration failed: ', err);
                    });
                  });
                } else {
                  console.debug('Service worker is not supported');
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
  val logger: Logger = LoggerFactory.getLogger("com.lemieuxdev.gameBoard")
  logger.debug("Rendering game board with game state: $gameState")
  id = "game-board"

  logger.debug("Applying current display render with adventure state and scene state")
  apply(currentDisplay.render(adventureState, currentScene.state))
  logger.debug("Game board rendered")
}

// Data models for Eleven Labs API
@Serializable
data class ElevenLabsTextToSpeechRequest(
    val text: String,
    val model_id: String = "eleven_monolingual_v1",
    val voice_settings: VoiceSettings = VoiceSettings()
)

@Serializable
data class VoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.5f
)

// HTTP client for Eleven Labs API
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}

// Function to call Eleven Labs API
suspend fun generateSpeech(text: String, voiceId: String = "21m00Tcm4TlvDq8ikWAM"): ByteArray {
    val logger: Logger = LoggerFactory.getLogger("com.lemieuxdev.generateSpeech")
    logger.debug("Generating speech for text: $text")

    // Get API key from environment variable
    val apiKey = System.getenv("ELEVEN_LABS_API_KEY") ?: throw IllegalStateException("ELEVEN_LABS_API_KEY environment variable is not set")

    val request = ElevenLabsTextToSpeechRequest(text = text)

    logger.debug("Sending request to Eleven Labs API")
    val response = httpClient.post("https://api.elevenlabs.io/v1/text-to-speech/$voiceId") {
        contentType(ContentType.Application.Json)
        header("xi-api-key", apiKey)
        setBody(request)
    }

    logger.debug("Received response from Eleven Labs API with status: ${response.status}")

    if (response.status.isSuccess()) {
        logger.debug("Successfully generated speech")
        return response.body()
    } else {
        logger.error("Failed to generate speech: ${response.bodyAsText()}")
        throw Exception("Failed to generate speech: ${response.status}")
    }
}

// HTML template for the text-to-speech form
fun HTML.textToSpeechForm() {
  head {
    title { +"Text to Speech" }
    meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
    link(rel = "stylesheet", href = "/static/output.css")
    script(src = "https://unpkg.com/htmx.org@2.0.4") {}
  }
  body {
    div {
      classes = "flex min-h-screen items-center justify-center bg-gray-900 p-4".split(" ").toSet()

      div {
        classes = "w-full max-w-md rounded-lg border-2 border-green-500 bg-black p-6 shadow-lg".split(" ").toSet()

        h1 {
          classes = "mb-6 text-2xl font-bold text-green-500".split(" ").toSet()
          +"Text to Speech Converter"
        }

        form {
          id = "tts-form"
          attributes["hx-post"] = "/text-to-speech"
          attributes["hx-target"] = "#audio-container"

          div {
            classes = "mb-4".split(" ").toSet()
            label {
              classes = "mb-2 block text-sm font-medium text-green-500".split(" ").toSet()
              htmlFor = "text-input"
              +"Enter text to convert to speech"
            }
            textArea {
              classes = "w-full rounded border border-green-500 bg-black p-2 text-green-500 focus:border-green-700 focus:outline-none".split(" ").toSet()
              id = "text-input"
              attributes["name"] = "text"
              attributes["rows"] = "5"
              attributes["required"] = "true"
              attributes["placeholder"] = "Type your text here..."
            }
          }

          button {
            classes = "w-full rounded bg-green-600 px-4 py-2 font-bold text-black hover:bg-green-700 focus:outline-none".split(" ").toSet()
            type = ButtonType.submit
            +"Convert to Speech"
          }
        }

        div {
          id = "audio-container"
          classes = "mt-6".split(" ").toSet()
        }
      }
    }
  }
}

fun Application.configureTemplating() {
  val logger: Logger = LoggerFactory.getLogger("com.lemieuxdev.Templating")
  logger.debug("Configuring templating")

  install(SSE)
  logger.debug("SSE installed")

  routing {
    logger.debug("Configuring routes")
    staticResources("/static", "static")
    staticResources("/", "/web")

    get("/") { 
      logger.debug("Handling GET request for root path")
      call.respondHtml { 
        logger.debug("Responding with HTML game board wrapper")
        gameBoardWrapper(game) 
      }
      logger.debug("Responded to GET request for root path")
    }

    get("/text-to-speech") {
      logger.debug("Handling GET request for text-to-speech form")
      call.respondHtml {
        textToSpeechForm()
      }
      logger.debug("Responded to GET request for text-to-speech form")
    }

    post("/text-to-speech") {
      logger.debug("Handling POST request for text-to-speech")

      // Get the text from the form
      val formParameters = call.receiveParameters()
      val text = formParameters["text"] ?: ""
      logger.debug("Received text: $text")

      if (text.isBlank()) {
        logger.debug("Text is blank, returning error")
        val errorHtml = createHTML().div {
          classes = "text-red-500".split(" ").toSet()
          +"Please enter some text to convert to speech."
        }
        call.respondText(errorHtml, ContentType.Text.Html)
        return@post
      }

      try {
        // Generate the speech audio
        logger.debug("Generating speech for text")
        val audioData = generateSpeech(text)
        logger.debug("Speech generated successfully, size: ${audioData.size} bytes")

        // Create a unique filename for the audio
        val filename = "speech_${UUID.randomUUID()}.mp3"
        logger.debug("Generated filename: $filename")

        // Save the audio data to a temporary file or serve it directly
        // For simplicity, we'll serve it directly with a data URL
        val base64Audio = Base64.getEncoder().encodeToString(audioData)
        val dataUrl = "data:audio/mpeg;base64,$base64Audio"
        logger.debug("Created data URL for audio")

        // Return the audio player HTML
        val audioPlayerHtml = createHTML().div {
          h3 {
            classes = "text-green-500 mb-2".split(" ").toSet()
            +"Generated Audio"
          }
          audio {
            classes = "w-full".split(" ").toSet()
            controls = true
            source {
              src = dataUrl
              type = "audio/mpeg"
            }
            +"Your browser does not support the audio element."
          }
          p {
            classes = "text-green-400 mt-2 text-sm".split(" ").toSet()
            +"Text: $text"
          }
        }

        call.respondText(audioPlayerHtml, ContentType.Text.Html)
        logger.debug("Responded with audio player HTML")
      } catch (e: Exception) {
        logger.error("Error generating speech: ${e.message}", e)
        val errorHtml = createHTML().div {
          classes = "text-red-500".split(" ").toSet()
          +"Error generating speech: ${e.message ?: "Unknown error"}"
        }
        call.respondText(errorHtml, ContentType.Text.Html)
      }
    }

    post("/action/{actionId}") {
      val actionId = call.parameters["actionId"]
      logger.debug("Received action request with ID: $actionId")
      println("Action ID: $actionId")

      // Find the matching action in the current scene state
      logger.debug("Looking for action in current scene state with ${currentScene.state.actions.size} actions")
      val action = currentScene.state.actions.find { it.name == actionId }

      if (action != null) {
        logger.debug("Action found: ${action.name}, executing")
        // Execute the action
        action.run()
        logger.debug("Action executed successfully")

        call.respondHtml(HttpStatusCode.OK) {}
        logger.debug("Responded with OK")
      } else {
        logger.debug("Action not found with ID: $actionId")
        call.respondHtml(HttpStatusCode.NotFound) { body { h1 { +"Action not found" } } }
        logger.debug("Responded with NotFound")
      }
    }

    sse("/events") { 
      logger.debug("SSE connection established")
      sseEvents.collect { event -> 
        logger.debug("Sending SSE event")
        send(ServerSentEvent(event)) 
      } 
    }
  }
}

/**
 * The main appliction here should be registering adventures, should be an interface, should take in
 * the event emiter
 */
