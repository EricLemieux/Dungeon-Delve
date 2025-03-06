package com.lemieuxdev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class Game(var foo: Int = 100, var player: Player?, var monster: Monster?, var outputText: String = "")
class Player(var attack: Int = 10)

fun Player.hitMonster(monster: Monster) {
    monster.health -= attack
}

class Monster(var health: Int = 50)

val game = Game(player = Player(), monster = Monster())

val sseEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)

fun HTML.gameBoardWrapper(gameState: Game) {
    head {
        title { +"Title" }
        link(rel = "stylesheet", href = "/static/output.css")
        script(src = "https://unpkg.com/htmx.org@2.0.4") {}
        script(src = "https://unpkg.com/htmx-ext-sse@2.2.2") {}
        script {
            unsafe {
                // language=javascript
                raw(
                    """
                      if (localStorage.theme === 'light') {
                        document.documentElement.classList.remove('dark')
                      } else {
                        document.documentElement.classList.add('dark')
                      }
                    """.trimIndent()
                )
            }
        }
    }
    body {
        attributes["hx-boost"] = "true"
        attributes["hx-ext"] = "sse"
        attributes["sse-connect"]="/events"
        attributes["sse-swap"]="message"
        attributes["hx-target"] = "#game-board"

        main {
            gameBoard(game)
        }
    }
}

fun MAIN.gameBoard(gameState: Game) {
    id = "game-board"

    div {
        classes = "flex min-h-screen items-center justify-center bg-gray-900 p-4".split(" ").toSet()

        div {
            classes = "relative w-full max-w-3xl overflow-hidden rounded-lg border-8 border-gray-800 bg-gray-900 shadow-2xl".split(" ").toSet()

            // Monitor frame gradient
            div {
                classes = "absolute inset-0 rounded-sm bg-gradient-to-b from-gray-800 to-gray-700 opacity-10".split(" ").toSet()
            }

            // Power indicator
            div {
                classes = "absolute right-4 top-4 flex items-center gap-2".split(" ").toSet()

                div {
                    classes = "h-2 w-2 rounded-full bg-green-500 shadow-[0_0_8px_2px_rgba(74,222,128,0.6)]".split(" ").toSet()
                }

                // Terminal icon placeholder (you might need to add an actual icon here)
                div {
                    classes = "h-4 w-4 text-gray-600".split(" ").toSet()
                }
            }

            // Screen with CRT effect
            div {
                classes = "relative overflow-hidden rounded-sm bg-black p-6 shadow-inner".split(" ").toSet()

                // CRT glow
                div {
                    classes = "pointer-events-none absolute inset-0 rounded-sm bg-green-500 opacity-[0.03] mix-blend-screen".split(" ").toSet()
                }

                // Scan lines
                div {
                    classes = "pointer-events-none absolute inset-0 bg-[linear-gradient(to_bottom,transparent,transparent_2px,rgba(0,0,0,0.05)_3px,transparent_3px)] bg-[length:100%_4px]".split(" ").toSet()
                }

                // Screen curvature
                div {
                    classes = "pointer-events-none absolute inset-0 rounded-sm bg-[radial-gradient(circle_at_center,transparent_30%,rgba(0,0,0,0.3)_100%)]".split(" ").toSet()
                }

                // Terminal content
                div {
                    classes = "relative min-h-[60vh] font-mono text-sm text-green-500 md:text-base".split(" ").toSet()

                    // Game controls
                    button {
                        attributes["hx-post"] = "/gamescreen/attack"
                        attributes["hx-swap"] = "none"
                        classes = "px-6 py-2 bg-green-600 hover:bg-green-700 text-black font-mono rounded border-2 border-green-500 transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-opacity-50 active:bg-green-800 mb-4".split(" ").toSet()
                        +"START MISSION"
                    }

                    // Output text
                    pre {
                        classes = "whitespace-pre-wrap".split(" ").toSet()
                        +gameState.outputText
                    }
                    // Blinking cursor
                    span {
                        classes = "inline-block h-4 w-2 animate-blink bg-green-500".split(" ").toSet()
                        +" "
                    }
                }
            }

            // Monitor base
            div {
                classes = "h-4 rounded-b-lg bg-gray-800".split(" ").toSet()
            }
        }
    }
}

fun Application.configureTemplating() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(SSE)

    routing {
        staticResources("/static", "static")
        staticResources("/", "/web")

        get("/") {
            call.respondHtml {
                gameBoardWrapper(game)
            }
        }

        post("/gamescreen/attack") {
            // todo: get the monster target somehow

            game.player?.hitMonster(game.monster!!)

            call.respondHtml(HttpStatusCode.OK) {}

            // TODO: move this somewhere elese, or make this part of the rendering
            val hardcodedText = "You awaken on a cold, uneven stone floor, the air thick with the damp scent of moss and earth. Faint echoes drip from unseen crevices, and the dim glow of phosphorescent fungi outlines jagged walls around you. As you rise, the weight of silence presses against your ears, broken only by the crunch of gravel beneath your boots. A narrow passage leads you forward, its walls narrowing before spilling you into blinding sunlight. Shielding your eyes, you step into the open and behold a vast, windswept desert stretching endlessly before you. Dominating the horizon stands a colossal pyramid, its golden surface shimmering under the sun, ancient and foreboding. The air hums with a strange energy, and a faint, unearthly whisper brushes your mind, urging you closer.".split(" ")

            repeat(hardcodedText.size) { i ->
                delay(0.05.seconds)
                game.outputText = hardcodedText.slice(0..i).joinToString(" ")
                val gameBoard = createHTML().main {
                    gameBoard(game)
                }
                sseEvents.emit(gameBoard)
            }
        }

        sse("/events") {
                sseEvents.collect { event ->
                   send(ServerSentEvent(event))
                }
        }
    }
}

/**
 * The main appliction here should be registering adventures, should be an interface, should take in the event emiter
 */
