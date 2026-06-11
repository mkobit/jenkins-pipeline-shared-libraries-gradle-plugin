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

rootProject.name = "peer-libraries-composite"

// deployer internally includes version-utils; Gradle composes nested included builds,
// so version-utils is available for GAV substitution without an explicit includeBuild here.
includeBuild("deployer")
includeBuild("notifier")
