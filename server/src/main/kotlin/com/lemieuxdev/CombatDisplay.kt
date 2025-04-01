package com.lemieuxdev

import com.lemieuxdev.Adventure.AdventureState
import com.lemieuxdev.Scene.SceneState
import kotlinx.html.*

/** Combat display for rendering combat scenes */
class CombatDisplay() : Display {
  override fun render(
      adventureState: AdventureState,
      sceneState: SceneState
  ): FlowContent.() -> Unit {
    return {
      div {
        classes = "flex min-h-screen items-center justify-center bg-gray-900 p-4".split(" ").toSet()

        div {
          classes =
              "relative w-full max-w-4xl overflow-hidden rounded-lg border-8 border-gray-800 bg-gray-900 shadow-2xl"
                  .split(" ")
                  .toSet()

          // Combat screen frame
          div {
            classes =
                "absolute inset-0 rounded-sm bg-gradient-to-b from-gray-800 to-gray-700 opacity-10"
                    .split(" ")
                    .toSet()
          }

          // Combat screen
          div {
            classes =
                "relative overflow-hidden rounded-sm bg-black p-6 shadow-inner".split(" ").toSet()

            // Screen effects
            div {
              classes =
                  "pointer-events-none absolute inset-0 rounded-sm bg-red-500 opacity-[0.03] mix-blend-screen"
                      .split(" ")
                      .toSet()
            }

            // Combat content
            div {
              classes =
                  "flex flex-col gap-4 relative min-h-[60vh] font-mono text-sm text-red-500 md:text-base"
                      .split(" ")
                      .toSet()

              // Combat header
              h2 {
                classes = "text-xl font-bold text-center mb-4".split(" ").toSet()
                +"COMBAT"
              }

              // Output text
              pre {
                classes = "whitespace-pre-wrap mb-4".split(" ").toSet()
                +sceneState.outputText
              }

              // Only render character lists if we have a CombatSceneState
              if (sceneState is CombatSceneState) {
                // Turn order section
                div {
                  classes = "mb-6".split(" ").toSet()
                  h3 {
                    classes = "text-lg font-bold mb-2 text-yellow-500".split(" ").toSet()
                    +"Turn Order"
                  }

                  // Character list in turn order
                  div {
                    classes = "space-y-2".split(" ").toSet()

                    for ((index, character) in sceneState.turnOrder.withIndex()) {
                      val isCurrentTurn = index == sceneState.currentTurnIndex
                      val borderColor =
                          if (character.isEnemy) "border-red-800" else "border-green-800"
                      val highlightClass =
                          if (isCurrentTurn) "bg-yellow-900 ring-2 ring-yellow-500" else ""
                      // Add animation class if this character was recently attacked
                      val animationClass =
                          if (character === sceneState.recentlyAttackedCharacter) "animate-damage"
                          else ""

                      div {
                        classes =
                            "border $borderColor p-2 rounded $highlightClass $animationClass"
                                .split(" ")
                                .toSet()

                        // Character name and health
                        div {
                          classes = "flex justify-between items-center".split(" ").toSet()

                          // Add turn indicator if it's this character's turn
                          if (isCurrentTurn) {
                            span {
                              classes = "flex items-center".split(" ").toSet()
                              span {
                                classes = "text-yellow-500 mr-2".split(" ").toSet()
                                +"▶ "
                              }
                              span { +"${character.name}" }
                            }
                          } else {
                            span { +"${character.name}" }
                          }

                          span { +"HP: ${character.health}" }
                        }

                        // Health bar
                        div {
                          classes = "w-full bg-gray-700 rounded-full h-2.5 mt-1".split(" ").toSet()
                          div {
                            val healthPercent =
                                (character.health.coerceAtLeast(0) * 100 / 100).coerceAtMost(100)
                            style = "width: ${healthPercent}%"
                            val barColor = if (character.isEnemy) "bg-red-600" else "bg-green-600"
                            classes = "$barColor h-2.5 rounded-full".split(" ").toSet()
                          }
                        }

                        // Attack info
                        div {
                          classes = "mt-1 text-xs".split(" ").toSet()
                          +"ATK: ${character.attack}"
                        }

                        // Type indicator
                        div {
                          classes = "mt-1 text-xs".split(" ").toSet()
                          val typeText = if (character.isEnemy) "Enemy" else "Friendly"
                          val typeColor =
                              if (character.isEnemy) "text-red-500" else "text-green-500"
                          span {
                            classes = typeColor.split(" ").toSet()
                            +typeText
                          }
                        }
                      }
                    }
                  }
                }

                // Game controls
                // Render buttons from scene state actions
                div {
                  classes = "mt-4 space-y-2".split(" ").toSet()

                  for (action in sceneState.actions) {
                    button {
                      attributes["hx-post"] = "/action/${action.name}"
                      attributes["hx-swap"] = "none"
                      classes =
                          "px-6 py-2 bg-red-600 hover:bg-red-700 text-black font-mono rounded border-2 border-red-500 transform hover:scale-105 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-opacity-50 active:bg-red-800 mb-4"
                              .split(" ")
                              .toSet()
                      +action.name
                    }
                  }
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
