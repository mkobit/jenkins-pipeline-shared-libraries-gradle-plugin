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

rootProject.name = "peer-libraries"

include(":deploy-pipeline", ":deploy-lib", ":shell-lib", ":checks-lib", ":metrics-lib")

// notify-lib is an included build peer library resolved via GAV notation.
includeBuild("notify-lib")
