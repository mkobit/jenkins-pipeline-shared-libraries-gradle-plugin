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
//
// Read as a -P project property, not an env var: env vars are only read by Gradle daemons at
// JVM startup on JDK 9+, so a reused daemon can silently miss a changed value. -P/-D values are
// resent with every build request and are documented to propagate into included builds (unlike
// gradle.properties), which this needs since dependency-submission-examples reaches this file
// as an included build via an example's pluginManagement.includeBuild("../..").
if (!providers.gradleProperty("dependencySubmission").getOrElse("false").toBoolean()) {
  file("examples")
    .listFiles { f -> f.isDirectory && f.resolve("settings.gradle.kts").exists() }
    .orEmpty()
    .forEach { includeBuild("examples/${it.name}") }
}
