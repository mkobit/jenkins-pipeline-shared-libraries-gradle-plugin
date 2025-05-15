pluginManagement {
  repositories {
    gradlePluginPortal()
    // TODO: remove this once we upgrade Nebula Release Plugin
    // NB: Included because grgit 3.1.1 is not available in Maven Central and Nebula depends on it.
    maven(url = "https://artifactory.appodeal.com/appodeal-public/")
  }
}

plugins {
  id("com.gradle.enterprise") version "3.3.4"
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"

apply(from = file("gradle/buildCache.settings.gradle.kts"))
