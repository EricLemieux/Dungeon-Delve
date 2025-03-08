plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("multiplatform") version "2.1.0" apply false
    id("io.ktor.plugin") version "3.0.3" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    group = "com.lemieuxdev"
    version = "0.0.1"

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }
    }
}
