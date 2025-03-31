plugins {
  kotlin("jvm")
  id("org.jetbrains.kotlin.plugin.serialization")
}

val ktor_version = "3.0.3"

dependencies {
  implementation("io.ktor:ktor-client-core:$ktor_version")
  implementation("io.ktor:ktor-client-cio:$ktor_version")
  implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
  implementation("io.ktor:ktor-client-logging:$ktor_version")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
  implementation("ch.qos.logback:logback-classic:1.4.11")

  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
