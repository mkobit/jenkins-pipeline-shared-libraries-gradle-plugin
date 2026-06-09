pluginManagement {
  includeBuild("../..") // the plugin itself
  repositories {
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    maven("https://repo.jenkins-ci.org/public/")
  }
}

rootProject.name = "deploy-pipeline"

// Subproject peer libraries — resolved via project() dependency notation.
// shell-lib is only declared as a peer of deploy-lib (src/-only, no vars/) and is picked up transitively.
include(":deploy-lib", ":shell-lib", ":checks-lib")

// Included build peer library — resolved via GAV notation with composite substitution.
includeBuild("notify-lib")
