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

rootProject.name = "library-a"

// included-lib-3 is a peer dependency of library-a; including it here makes Gradle
// substitute com.example.pipeline:included-lib-3 with the local project in all builds
// that include library-a.
includeBuild("../included-lib-3")
