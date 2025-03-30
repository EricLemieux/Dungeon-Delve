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
                // Characters section
                div {
                  classes = "flex flex-row justify-between gap-8 mb-6".split(" ").toSet()

                  // Enemies section
                  div {
                    classes = "flex-1".split(" ").toSet()
                    h3 {
                      classes = "text-lg font-bold mb-2 text-red-600".split(" ").toSet()
                      +"Enemies"
                    }
                    
                    // Enemy list
                    div {
                      classes = "space-y-2".split(" ").toSet()
                      
                      for (enemy in sceneState.getEnemies()) {
                        div {
                          classes = "border border-red-800 p-2 rounded".split(" ").toSet()
                          
                          // Enemy name and health
                          div {
                            classes = "flex justify-between items-center".split(" ").toSet()
                            span { +"${enemy.name}" }
                            span { +"HP: ${enemy.health}" }
                          }
                          
                          // Health bar
                          div {
                            classes = "w-full bg-gray-700 rounded-full h-2.5 mt-1".split(" ").toSet()
                            div {
                              val healthPercent = (enemy.health.coerceAtLeast(0) * 100 / 100).coerceAtMost(100)
                              style = "width: ${healthPercent}%"
                              classes = "bg-red-600 h-2.5 rounded-full".split(" ").toSet()
                            }
                          }
                        }
                      }
                    }
                  }
                  
                  // Friendlies section
                  div {
                    classes = "flex-1".split(" ").toSet()
                    h3 {
                      classes = "text-lg font-bold mb-2 text-green-600".split(" ").toSet()
                      +"Friendlies"
                    }
                    
                    // Friendly list
                    div {
                      classes = "space-y-2".split(" ").toSet()
                      
                      for (friendly in sceneState.getFriendlies()) {
                        div {
                          classes = "border border-green-800 p-2 rounded".split(" ").toSet()
                          
                          // Friendly name and health
                          div {
                            classes = "flex justify-between items-center".split(" ").toSet()
                            span { +"${friendly.name}" }
                            span { +"HP: ${friendly.health}" }
                          }
                          
                          // Health bar
                          div {
                            classes = "w-full bg-gray-700 rounded-full h-2.5 mt-1".split(" ").toSet()
                            div {
                              val healthPercent = (friendly.health.coerceAtLeast(0) * 100 / 100).coerceAtMost(100)
                              style = "width: ${healthPercent}%"
                              classes = "bg-green-600 h-2.5 rounded-full".split(" ").toSet()
                            }
                          }
                          
                          // Attack info
                          div {
                            classes = "mt-1 text-xs".split(" ").toSet()
                            +"ATK: ${friendly.attack}"
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