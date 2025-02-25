package com.lemieuxdev

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class Game(var foo: Int = 100, var player: Player?, var monster: Monster?)
class Player(var attack: Int = 10)

fun Player.hitMonster(monster: Monster) {
    monster.health -= attack
}

class Monster(var health: Int = 50)

val game = Game(player = Player(), monster = Monster())

fun HTML.gameBoardWrapper(gameState: Game) {
    head {
        title { +"Title" }
        link(rel = "stylesheet", href = "/static/output.css")
        script(src = "https://unpkg.com/htmx.org@2.0.4") {}
        script(src = "https://unpkg.com/htmx.org@1.9.12/dist/ext/ws.js") {}
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

        main {
            gameBoard(game)
        }
    }
}

fun MAIN.gameBoard(gameState: Game) {
    id = "game-board"
    attributes["hx-ext"] = "ws"
    attributes["ws-connect"] = "/ws"

    div {
        +"Monster health: ${gameState.monster?.health}"
    }

    // game
    div {
        // attack
        button {
            attributes["hx-post"] = "/gamescreen/attack"
            attributes["hx-target"] = "#game-board"
            classes =
                "px-6 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-lg shadow-md transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 active:bg-red-800".split(
                    " "
                ).toSet()
            +"Attack"
        }

        // move
        div {
            p {
                +"${LocalDateTime.now()}"
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

            call.respondHtml {
                gameBoardWrapper(game)
            }
        }

        webSocket("/ws") {
            launch {
                val html = createHTML().main {
                    gameBoard(game)
                }
                send(Frame.Text(html.toString()))
            }

            // Handle incoming messages (optional)
//            incoming.consumeEach { frame ->
//                if (frame is Frame.Text) {
//                    println("Received: ${frame.readText()}")
//                }
//            }
        }
    }
}
