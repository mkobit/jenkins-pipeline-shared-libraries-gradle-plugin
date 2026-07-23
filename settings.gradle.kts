pluginManagement {
  includeBuild("build-logic")
  repositories {
    gradlePluginPortal()
  }
}

plugins {
  id("com.gradle.develocity") version "4.4.3"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"
    publishing.onlyIf { System.getenv("DEVELOCITY_PUBLISH") == "1" }
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    maven("https://repo.jenkins-ci.org/public/")
  }
}

rootProject.name = "jenkins-pipeline-shared-libraries-gradle-plugin"

include("examples")

// Each examples/* build includes this build back via pluginManagement.includeBuild("../..")
// to resolve the plugin from source. Composite-build tooling (e.g. the dependency-submission
// action's ForceDependencyResolutionPlugin) walks gradle.includedBuilds unconditionally, so
// including every example here too turns that mutual reference into a literal task cycle.
// Skip this forward side of the loop for dependency-submission runs, which only need this
// build's own graph and don't use the example-runner tasks below.
if (System.getenv("GRADLE_DEPENDENCY_SUBMISSION") != "true") {
  file("examples")
    .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
    .orEmpty()
    .forEach { includeBuild("examples/${it.name}") }
}
