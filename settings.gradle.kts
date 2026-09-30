pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.4.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
        id("com.google.devtools.ksp") version "2.3.12"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "card_locator"
include(":app")
