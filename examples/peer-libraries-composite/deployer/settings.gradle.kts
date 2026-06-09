pluginManagement {
  includeBuild("../../..") // the plugin itself
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

rootProject.name = "deployer"

// version-utils is a peer dependency of deployer; including it here makes Gradle
// substitute com.example.pipeline:version-utils with the local project in all builds
// that include deployer.
includeBuild("../version-utils")
