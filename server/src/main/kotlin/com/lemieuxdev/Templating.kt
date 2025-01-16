package com.lemieuxdev

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.html.*
import org.slf4j.event.*

class Game(var foo: Int = 100, var player: Player?, var monster: Monster?)
class Player(var attack:Int=10)
fun Player.hitMonster(monster: Monster) {
    monster.health-=attack
}
class Monster(var health: Int = 50)
val game = Game(player = Player(), monster = Monster())

fun Application.configureTemplating() {
    routing {
        val random = Random(System.currentTimeMillis())

        get("/") {
            call.respondHtml {
                body{
                    h1{
                        +"The monster has ${game.monster?.health} HP"
                    }
                }
            }

            game.player?.hitMonster(game.monster!!)
        }

        get("/more-rows") {
            call.respondHtml {
                body {
                    table {
                        tbody {
                            +game.foo
                        }
                    }
                }
            }
        }
    }
}
