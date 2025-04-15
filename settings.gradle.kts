plugins {
  id("com.gradle.enterprise") version "3.19.2"
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"

apply(from = file("gradle/buildCache.settings.gradle.kts"))
