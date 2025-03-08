val kotlin_version: String by project
val kotlinx_browser_version: String by project
val kotlinx_html_version: String by project
val logback_version: String by project

plugins {
  kotlin("jvm") version "2.1.0"
  id("io.ktor.plugin") version "3.0.3"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

application {
  mainClass.set("io.ktor.server.netty.EngineMain")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.withType<ProcessResources> {
  from("../web/build/dist/wasmJs/productionExecutable") {
    into("web")
    include("**/*")
  }
  duplicatesStrategy = DuplicatesStrategy.WARN
}

dependencies {
  implementation("io.ktor:ktor-server-core-jvm")
  implementation("io.ktor:ktor-server-auth-jvm")
  implementation("io.ktor:ktor-server-host-common-jvm")
  implementation("io.ktor:ktor-server-call-logging-jvm")
  implementation("io.ktor:ktor-server-content-negotiation-jvm")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
  implementation("io.ktor:ktor-server-html-builder-jvm")
  implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
  implementation("io.ktor:ktor-server-websockets-jvm")
  implementation("io.ktor:ktor-server-netty-jvm")
  implementation("ch.qos.logback:logback-classic:$logback_version")
  implementation("io.ktor:ktor-server-config-yaml-jvm")
  implementation("io.ktor:ktor-server-sse")
  testImplementation("io.ktor:ktor-server-test-host-jvm")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
