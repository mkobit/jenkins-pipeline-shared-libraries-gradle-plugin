plugins {
  id("com.gradle.enterprise") version "3.3.4"
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"

apply(from = file("gradle/buildCache.settings.gradle.kts"))
