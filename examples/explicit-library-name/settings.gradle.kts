pluginManagement {
  includeBuild("../..")
  repositories {
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    maven("https://repo.jenkins-ci.org/public/")
  }
}

rootProject.name = "explicit-library-name"
