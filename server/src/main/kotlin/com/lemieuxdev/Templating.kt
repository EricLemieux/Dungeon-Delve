package com.lemieuxdev

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlin.random.Random
import kotlinx.html.*

class Game(var foo: Int = 100, var player: Player?, var monster: Monster?)
class Player(var attack: Int = 10)

fun Player.hitMonster(monster: Monster) {
    monster.health -= attack
}

class Monster(var health: Int = 50)

val game = Game(player = Player(), monster = Monster())

fun HTML.gameBoard(gameState: Game) {
    head {
        title { +"Title" }
        link(rel = "stylesheet", href = "/static/output.css")
        script(src = "https://unpkg.com/htmx.org@2.0.4") {}
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
                    """
                )
            }
        }
    }
    body {
        attributes["hx-boost"] = "true"

        main{
            id = "game-board"

            div {
                +"Monster health: ${gameState.monster?.health}"
            }

            // game
            div {
                // attack
                button {
                    attributes["hx-post"] = "/gamescreen/attack"
                    attributes["hx-target"] = "#game-board"
                    classes = "px-6 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded-lg shadow-md transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 active:bg-red-800".split(" ").toSet()
                    +"Attack"
                }

                // move
                div {
                }
            }
        }
    }
}

fun Application.configureTemplating() {
    routing {
        val random = Random(System.currentTimeMillis())

        get("/") {
            call.respondHtml {
                gameBoard(game)
            }
        }

        post("/gamescreen/attack") {
            // todo: get the monster target somehow

            game.player?.hitMonster(game.monster!!)

            call.respondHtml {
                gameBoard(game)
            }
        }
    }
}
