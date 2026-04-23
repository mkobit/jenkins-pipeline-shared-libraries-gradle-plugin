plugins {
  `kotlin-dsl`
  id("com.gradle.plugin-publish") version "2.1.1"
  id("com.diffplug.spotless") version "8.4.0"
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

dependencies {
  api(gradleApi())
  implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  testImplementation(kotlin("reflect"))
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
  testImplementation("io.kotest:kotest-framework-datatest:5.9.1")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
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
    target("*.gradle.kts", "buildSrc/**/*.gradle.kts")
  }
}
