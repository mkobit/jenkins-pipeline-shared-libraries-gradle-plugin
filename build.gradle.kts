plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish") version "2.1.1"
  id("com.diffplug.spotless") version "8.4.0"
  id("org.openrewrite.rewrite") version "6.26.0"
}

group = "com.mkobit.jenkins.pipelines"
description = "Gradle plugins for Jenkins Pipeline shared library development and testing"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}

gradlePlugin {
  website = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"
  vcsUrl = "https://github.com/mkobit/jenkins-pipeline-shared-libraries-gradle-plugin"
  plugins {
    create("sharedLibrary") {
      id = "com.mkobit.jenkins.pipelines.shared-library"
      implementationClass = "com.mkobit.jenkins.pipelines.SharedLibraryPlugin"
      displayName = "Jenkins Pipeline Shared Library Development"
      description = "Configures and sets up a Gradle project for development and testing of a Jenkins Pipeline shared library"
      tags = listOf("jenkins", "pipeline", "shared library")
    }
    create("jenkinsIntegration") {
      id = "com.mkobit.jenkins.pipelines.jenkins-integration"
      implementationClass = "com.mkobit.jenkins.pipelines.JenkinsIntegrationPlugin"
      displayName = "Jenkins Integration Plugin"
      description = "Tasks to retrieve information from a Jenkins instance to aid in development of Gradle tooling"
      tags = listOf("jenkins")
    }
  }
}

rewrite {
  activeRecipe("org.openrewrite.gradle.MigrateToGradle9")
}

dependencies {
  api(gradleApi())
  implementation(libs.kotlin.logging)
  implementation(libs.okhttp)

  testImplementation(kotlin("reflect"))
  testImplementation(libs.mockk)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.kotest.assertions)
  testImplementation(libs.kotest.datatest)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(libs.versions.junit.jupiter.get())
    }
  }
}

spotless {
  kotlin {
    ktlint()
    target("src/**/*.kt")
  }
  kotlinGradle {
    ktlint()
    target("*.gradle.kts")
  }
}
