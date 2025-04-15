@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://repo.jenkins-ci.org/public/")
  }
}

plugins {
  id("com.gradle.develocity") version "3.19.2"
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"

apply(from = file("gradle/buildCache.settings.gradle.kts"))
