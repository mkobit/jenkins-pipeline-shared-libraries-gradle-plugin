plugins {
  `kotlin-dsl`
  alias(libs.plugins.gradle.publish)
  alias(libs.plugins.openrewrite)
  alias(libs.plugins.spotless)
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
  }
}

dependencies {
  api(gradleApi())
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(
        libs.versions.junit.jupiter
          .get(),
      )
      dependencies {
        implementation(libs.mockk)
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.runner)
      }
    }

    val functionalTest by registering(JvmTestSuite::class) {
      useJUnitJupiter(
        libs.versions.junit.jupiter
          .get(),
      )
      dependencies {
        implementation(gradleTestKit())
        implementation(libs.kotest.assertions)
        implementation(libs.kotest.runner)
      }
      targets.all {
        testTask.configure {
          mustRunAfter(test)
          // java-gradle-plugin only wires pluginUnderTestMetadata into the test suite;
          // add it explicitly so GradleRunner.withPluginClasspath() works here.
          classpath += files(tasks.named("pluginUnderTestMetadata"))
          // Resolution tests hit the Jenkins Maven repo and are slow on a cold cache.
          // Default to excluding them from the normal check so CI can run them as a
          // separate cacheable step. Override with -Pkotest.tags=resolution (or any
          // other Kotest tag expression) to target a specific subset.
          systemProperty(
            "kotest.filter.tags",
            project.findProperty("kotest.tags") ?: "!resolution",
          )
        }
      }
    }
  }
}

tasks.named("check") {
  dependsOn(testing.suites.named("functionalTest"))
}

tasks.wrapper {
  gradleVersion = "9.4.1"
  distributionType = Wrapper.DistributionType.ALL
}

spotless {
  kotlin {
    ktlint()
    target("src/**/*.kt")
    targetExclude("src/integrationTest/**/*.kt")
  }
  kotlinGradle {
    ktlint()
    target("*.gradle.kts")
  }
}
